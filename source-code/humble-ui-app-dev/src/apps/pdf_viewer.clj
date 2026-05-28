(ns apps.pdf-viewer
  (:require [clojure.string :as str]
            [io.github.humbleui.ui :as ui])
  (:import [java.awt.image BufferedImage]
           [java.io ByteArrayOutputStream File]
           [javax.imageio ImageIO]
           [javax.swing JFileChooser]
           [javax.swing.filechooser FileNameExtensionFilter]
           org.apache.pdfbox.Loader
           org.apache.pdfbox.rendering.PDFRenderer))

;; ── State ───────────────────────────────────────────────────────

(def *page-bytes  (ui/signal nil))
(def *page-count  (ui/signal 0))
(def *page-num    (ui/signal 1))
(def *file-path   (ui/signal nil))
(def *error-msg   (ui/signal nil))
(def ^:private *pdf-doc (ui/signal nil))

;; ── PDF Logic ────────────────────────────────────────────────────

(defn- render-page [doc page-num]
  (let [renderer (PDFRenderer. doc)
        scale    1.5
        dpi      (* 72 scale)
        img      (.renderImageWithDPI renderer (dec page-num) dpi)
        baos     (ByteArrayOutputStream.)]
    (ImageIO/write img "png" baos)
    (.toByteArray baos)))

(defn- load-pdf [file]
  (try
    (let [doc   (Loader/loadPDF file)
          pages (.getNumberOfPages doc)]
      (reset! *error-msg nil)
      (reset! *pdf-doc doc)
      (reset! *page-count pages)
      (reset! *page-num 1)
      (reset! *file-path (.getName file))
      (reset! *page-bytes (render-page doc 1)))
    (catch Exception e
      (reset! *error-msg (str "Failed to load PDF: " (.getMessage e)))
      (reset! *pdf-doc nil)
      (reset! *page-count 0))))

(defn- go-to-page [n]
  (when-let [doc @*pdf-doc]
    (let [n (max 1 (min @*page-count n))]
      (reset! *page-num n)
      (reset! *page-bytes (render-page doc n)))))

(defn- choose-and-load-pdf []
  (let [chooser (JFileChooser.)]
    (.setDialogTitle chooser "Open PDF File")
    (.setFileFilter chooser (FileNameExtensionFilter. "PDF Files" (into-array String ["pdf"])))
    (when (= JFileChooser/APPROVE_OPTION (.showOpenDialog chooser nil))
      (let [file (.getSelectedFile chooser)]
        (load-pdf file)))))

;; ── UI Components ────────────────────────────────────────────────

(ui/defcomp toolbar []
  [ui/rect {:paint {:fill 0xFFF0F0F0}}
   [ui/padding {:horizontal 12 :vertical 8}
    [ui/row {:gap 12}
     [ui/button {:on-click (fn [_] (choose-and-load-pdf))}
      [ui/label "Open PDF"]]
     (when (pos? @*page-count)
       [ui/row {:gap 8}
        [ui/button {:on-click (fn [_] (go-to-page (dec @*page-num)))}
         [ui/label "Prev"]]
        [ui/label {:font-size 14}
         (str "Page " @*page-num " / " @*page-count)]
        [ui/button {:on-click (fn [_] (go-to-page (inc @*page-num)))}
         [ui/label "Next"]]])
     (when @*file-path
       [ui/label {:paint {:fill 0xFF666666} :font-size 12}
        @*file-path])]]])

(ui/defcomp pdf-view []
  [ui/rect {:paint {:fill 0xFFE0E0E0}}
   (if @*page-bytes
     [ui/image {:src @*page-bytes :scale :fit}]
     [ui/center
      [ui/column {:gap 10}
       (if @*error-msg
         [ui/label {:paint {:fill 0xFFCC0000}} @*error-msg]
         [ui/label {:paint {:fill 0xFF999999} :font-size 18}
          "No PDF loaded. Click 'Open PDF' to select a file."])]])])

(ui/defcomp ui []
  [ui/column
   [toolbar]
   [ui/rect {:paint {:stroke 0xFFD0D0D0}}
    [ui/gap {:height 1}]]
   ^{:stretch 1}
   [pdf-view]])

(defn -main [& args]
  (ui/start-app!
    (ui/window
      {:title "PDF Viewer"
       :width 800
       :height 700}
      #'ui)))
