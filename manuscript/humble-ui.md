# Building Interactive Desktop Apps with Clojure and Humble UI

This chapter explores building functional, interactive desktop applications using Clojure and the ---Humble UI--- library. We will transition from a simple "Hello World" greeting application to more complex tools, including an AI-powered chat client and a PDF viewer.

## Core Concepts of Humble UI

Humble UI provides a declarative way to build user interfaces in Clojure. It uses a concept called **signals** for state management and **components** for building the UI structure.

### State Management with Signals

Signals are reactive pieces of state that notify components when their value changes. In the examples below, you will see how we use `ui/signal` to define state and `reset!` or `swap!` to update it.

```clojure
(def *name (ui/signal {:text ""})) ;; A signal containing a map
(def *messages (ui/signal []))      ;; A signal containing a vector
```

### Declarative UI Components

Components are defined using the `ui/defcomp` macro. They describe *what* the UI should look like based on the current state, rather than *how* to change it.

## 1. The Basic Greeting App

We start with a simple example in `src/apps/hello.clj`. This application demonstrates how to capture user input and reactively display a greeting.

### Implementation Details

The application consists of:
- **State**: A signal for the user's name and another for the greeting message.
- **Logic**: A `greet` function that reads the current name and updates the greeting.
- **UI Structure**: A vertical column containing a label, a text field tied to the name signal, a button to trigger the greeting, and the resulting greeting display.

Listing of **humble-ui-app-dev/src/apps/hello.clj**:

```clojure
(ns apps.hello
  (:require [io.github.humbleui.ui :as ui]))

(def *name (ui/signal {:text ""}))

(def *greeting (ui/signal ""))

(defn greet []
  (let [n (:text @*name)]
    (reset! *greeting
      (if (clojure.string/blank? n)
        "Please enter your name first!"
        (str "Hello, " n "!")))))

(ui/defcomp ui []
  [ui/center
   [ui/padding {:padding 30}
    [ui/column {:gap 15}
     [ui/align {:x :center}
      [ui/label {:font-size 28 :font-weight :bold} "Hello Humble UI"]]
     [ui/gap {:height 10}]
     [ui/label {:font-size 16} "Enter your name:"]
     [ui/size {:width 280}
      [ui/text-field {:*state *name}]]
     [ui/gap {:height 5}]
     [ui/align {:x :center}
      [ui/button {:on-click (fn [_] (greet))}
       [ui/label {:font-size 16} "Greet"]]]
     [ui/gap {:height 5}]
     [ui/align {:x :center}
      [ui/label {:font-size 20 :font-weight 500} *greeting]]]]])

(defn -main [& args]
  (ui/start-app!
    (ui/window
      {:title "Hello Humble UI"
       :width 500
       :height 400}
      #'ui)))
```

## 2. AI Chat Client: Integrating External APIs

Moving up in complexity, `src/apps/chat.clj` implements a chat interface that communicates with Large Language Models like Gemini, OpenAI, or local models via Ollama.

### Key Features

- **Multi-Provider Support**: The application allows users to toggle between different AI backends at runtime.
- **Asynchronous Communication**: API calls are wrapped in `future` to prevent the UI from freezing while waiting for a response.
- **Rich History Management**: It maintains a conversation history, passing it to the APIs to allow for context-aware dialogue.

Listing of **humble-ui-app-dev/src/apps/chat.clj**:

```clojure
(ns apps.chat
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [io.github.humbleui.ui :as ui])
  (:import [java.net.http HttpClient HttpRequest HttpRequest$BodyPublishers HttpResponse$BodyHandlers]
           [java.net URI]))

;; ── State ───────────────────────────────────────────────────────

(def *messages  (ui/signal []))
(def *input     (ui/signal {:text ""}))
(def *api       (ui/signal :gemini))
(def *loading?  (ui/signal false))

;; ── API Helpers ──────────────────────────────────────────────────

(defn gemini-api-key [] (System/getenv "GOOGLE_API_KEY"))
(defn openai-api-key [] (System/getenv "OPENAI_API_KEY"))
(def ^:private http-client (HttpClient/newHttpClient))

(defn- http-post [url headers body]
  (let [builder (HttpRequest/newBuilder (URI. url))]
    (.POST builder (HttpRequest$BodyPublishers/ofString body))
    (doseq [[k v] headers]
      (.header builder k v))
    (let [req     (.build builder)
          resp    (.send http-client req (HttpResponse$BodyHandlers/ofString))
          status  (.statusCode resp)]
      (if (<= 200 status 299)
        (.body resp)
        (throw (ex-info (str "HTTP " status ": " (.body resp))
                        {:status status}))))))

(defn- call-gemini [prompt history]
  (let [api-key (gemini-api-key)]
    (when-not api-key
      (throw (ex-info "GOOGLE_API_KEY environment variable not set" {})))
    (let [contents (->> (conj history {:role :user :content prompt})
                        (mapv (fn [m]
                                {:role  (if (= :assistant (:role m)) "model" "user")
                                 :parts [{:text (:content m)}]})))
          body     (json/write-str {:contents contents})
          url      (str "https://generativelanguage.googleapis.com/v1beta/models/"
                        "gemini-2.0-flash:generateContent?key=" api-key)
          resp     (http-post url {"Content-Type" "application/json"} body)
          data     (json/read-str resp :key-fn keyword)]
      (-> data :candidates first :content :parts first :text))))

(defn- call-openai [prompt history]
  (let [api-key (openai-api-key)]
    (when-not api-key
      (throw (ex-info "OPENAI_API_KEY environment variable not set" {})))
    (let [messages (->> (conj history {:role :user :content prompt})
                        (mapv #(select-keys % [:role :content])))
          body     (json/write-str {:model    "gpt-4o-mini"
                                    :messages messages})
          resp     (http-post "https://api.openai.com/v1/chat/completions"
                              {"Content-Type"  "application/json"
                               "Authorization" (str "Bearer " api-key)}
                              body)
          data     (json/read-str resp :key-fn keyword)]
      (-> data :choices first :message :content))))

(defn- call-ollama [prompt history]
  (let [messages (->> (conj history {:role :user :content prompt})
                      (mapv #(select-keys % [:role :content])))
        body     (json/write-str {:model    "phi3:latest"
                                  :messages messages
                                  :stream   false})
        resp     (http-post "http://localhost:11434/api/chat"
                            {"Content-Type" "application/json"}
                            body)
        data     (json/read-str resp :key-fn keyword)]
    (:message data)))

(defn- send-message []
  (let [text (str/trim (:text @*input))]
    (when (and (not (str/blank? text)) (not @*loading?))
      (swap! *messages conj {:role :user :content text})
      (swap! *input assoc :text "")
      (reset! *loading? true)
      (let [history @*messages
            api     @*api]
        (future
          (try
            (let [response (case api
                             :gemini (call-gemini text history)
                             :openai (call-openai text history)
                             :ollama (call-ollama text history))]
              (swap! *messages conj {:role :assistant :content response}))
            (catch Exception e
              (swap! *messages conj
                {:role :assistant
                 :content (str "Error: " (.getMessage e))}))
            (finally
              (reset! *loading? false))))))))

;; ── UI Components ────────────────────────────────────────────────

(def api-options
  [{:key :gemini :label "Gemini"}
   {:key :openai :label "OpenAI"}
   {:key :ollama :label "Ollama"}])

(ui/defcomp api-toggle-btn [api-option]
  (let [selected? (= (:key api-option) @*api)]
    [ui/clickable
     {:on-click (fn [_] (reset! *api (:key api-option)))}
     (fn [state]
       [ui/rect {:radius 4
                 :paint  {:fill (cond
                                  (:pressed state) 0xFFD0D0D0
                                  selected?         0xFFB2D7FE
                                  (:hovered state)  0xFFE1EFFA
                                  :else             0xFFF0F0F0)}}
        [ui/padding {:horizontal 16 :vertical 6}
         [ui/label {:font-weight (if selected? :bold 400)}
          (:label api-option)]]])]))

(ui/defcomp api-toggle []
  [ui/row {:gap 4}
   (for [opt api-options]
     [api-toggle-btn opt])])

(ui/defcomp chat-message [msg]
  (let [role (:role msg)]
    [ui/padding {:horizontal 12 :vertical 4}
     [ui/row {:gap 8}
      [ui/size {:width 60}
       [ui/label {:font-weight :bold
                  :paint       {:fill (if (= :user role) 0xFF2196F3 0xFF4CAF50)}}
        (if (= :user role) "You:" "AI:")]]
      [ui/label (:content msg)]]]))

(ui/defcomp chat-history []
  [ui/vscroll
   [ui/padding {:bottom 8}
    [ui/column
     (if (empty? @*messages)
       [ui/padding {:padding 20}
        [ui/label {:paint {:fill 0xFF999999}}
         "Start a conversation by typing a message below."]]
       (for [[i msg] (map-indexed vector @*messages)]
         ^{:key i} [chat-message msg]))]]])

(ui/defcomp input-area []
  [ui/rect {:paint {:fill 0xFFF5F5F5}}
   [ui/padding {:horizontal 8 :vertical 8}
    [ui/row {:gap 8}
     ^{:stretch 1}
     [ui/text-field {:*state *input}]
     [ui/button {:on-click (fn [_] (send-message))}
      (if @*loading?
        [ui/label {:paint {:fill 0xFF999999}} "Sending..."]
        [ui/label "Send"])]]]])

(ui/defcomp ui []
  [ui/column
   [ui/padding {:horizontal 12 :vertical 8}
    [ui/rect {:paint {:fill 0xFFFAFAFA}}
     [ui/row {:gap 12}
      [ui/label {:font-weight :bold :font-size 14} "AI Chat Client"]
      [ui/gap {:width 20}]
      [api-toggle]]]]
   [ui/rect {:paint {:stroke 0xFFE0E0E0}}
    [ui/gap {:height 1}]]
   ^{:stretch 1}
   [chat-history]
   [input-area]])

(defn -main [& args]
  (ui/start-app!
    (ui/window
      {:title "AI Chat Client"
       :width 650
       :height 550}
      #'ui)))
```


### The Chat Loop

When a user sends a message:
1. The message is added to the local state (`*messages`).
2. A background thread (`future`) is spawned to perform the HTTP request.
3. Once the API responds, the assistant's reply is appended to `*messages`.
4. The UI, being reactive, automatically re-renders to show the new messages.

## 3. PDF Viewer: Handling Files and Graphics

Finally, we look at `src/apps/pdf_viewer.clj`, which demonstrates how to integrate Java libraries like Apache PDFBox with Humble UI to create a functional utility.

### Rendering PDFs as Images

Since graphical UIs often prefer working with bitmapped data, this application converts PDF pages into images on the fly:
- **PDFBox**: Used to load and parse the PDF file.
- **Java AWT/ImageIO**: Processes the rendered page from PDFBox and converts it into a byte array representing a PNG image.
- **Humble UI `ui/image`**: Displays the resulting byte array within the application window.

### Navigation and Workflow

The app provides a toolbar with:
- **File Loading**: Uses `JFileChooser` to allow users to select a file from their system.
- **Pagination**: Buttons to move between pages, leveraging state to keep track of the current page number and total count.

Listing of **humble-ui-app-dev/src/apps/pdf_viewer.clj**:

```clojure
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
```

## Summary

Through these three examples, we have covered:
1. Basic reactive input and display.
2. Interacting with web services via HTTP in a non-blocking way.
3. Bridging standard Java desktop libraries (AWT/Swing) with a modern declarative UI framework.

With Humble UI, the boundary between simple scripts and full-featured desktop applications becomes incredibly thin, allowing you to focus on your application logic rather than the intricacies of GUI event loops.
