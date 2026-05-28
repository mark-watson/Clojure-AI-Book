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
