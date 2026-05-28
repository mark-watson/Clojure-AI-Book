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
