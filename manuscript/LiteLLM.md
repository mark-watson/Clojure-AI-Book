# Using the LiteLLM-CLJ Library as a Universal LLM Interface

Earlier LLM examples mostly implemented Clojure code using low-level libraries to access REST interfaces. Here we use a new project tat as of December 2025 is still a work in progress.

For documentation please refer to the GitHub repository for the LiteLLM-CLJ library: [https://github.com/unravel-team/litellm-clj](https://github.com/unravel-team/litellm-clj).

*Note: this chapter is a work in progress until the LiteLLM-CLJ library is out of alpha-test status.*

The code snippets for this work in progress chapter are fount in the Clojure tests directory **Clojure-AI-Book-Code/litellm_api/test/litellm_api**.

## OpenAI Completions Test

TBD

```clojure
(deftest openai-completions-test
  (testing "OpenAI completions API with LiteLLM"
   (router/register!
    :fast
    {:provider :openai
     :model "gpt-4o-mini"
     :config {:api-key (System/getenv "OPENAI_API_KEY")}})
   (let [response (router/completion :fast
                   {:messages [{:role :user :content "please generate a 10 word sentence"}]})]
     (println (router/extract-content response))
     (is (not (nil? response))))))
```

TBD

Here we run just this single test:

```bash
$ lein test :only litellm-api.core-test/openai-completions-test

lein test litellm-api.core-test
17:08:43.891 [main] INFO litellm.config -- Registered configuration {:config-name :fast}
The sun set slowly, casting beautiful colors across the sky.

Ran 1 tests containing 1 assertions.
0 failures, 0 errors.
```

If you print the full response it will look like:

```json
{:id chatcmpl-ClObHmxxgRV8LeNh7aQm8QRSzn0BU,
 :object chat.completion,
 :created 1765412015,
 :model gpt-4o-mini-2024-07-18,
 :choices ({:index 0, :message {:role :assistant, :content The sun set behind the mountains, painting the sky orange.},
 :finish-reason :stop}),
 :usage {:prompt-tokens 14, :completion-tokens 12, :total-tokens 26}}
```



## Google Gemini Completions Test

TBD

```clojure

(deftest google-completions-test
  (testing "Google Gemini completions API with LiteLLM"
   (router/register!
    :fast
    {:provider :gemini
     :model "gemini-2.5-flash"
     :config {:api-key (System/getenv "GOOGLE_API_KEY")}})
   (let [response (router/completion :fast
                   {:messages [{:role :user :content "please generate a 10 word sentence"}]})]
     (println (router/extract-content response))
     (is (not (nil? response))))))
 ```
 
 TBD
 
 Here we run just this single test:

```bash
$ lein test :only litellm-api.core-test/google-completions-test

lein test litellm-api.core-test
17:09:45.844 [main] INFO litellm.config -- Registered configuration {:config-name :fast}
The quick brown fox jumps over the lazy dog.

Ran 1 tests containing 1 assertions.
0 failures, 0 errors.
```

If you print the full response it will look like:

```json
{:id The sun shines bright, warming the earth and sky above.,
 :object chat.completion,
 :created 1765411883,
 :model gemini-unknown,
 :choices ({:index 0, :message {:role :assistant, :content The sun shines bright, warming the earth and sky above., :tool-calls ({:id 07910493-6a4c-46ed-87b7-e2b8f81f5164, :type function, :function {:name nil, :arguments null}})}, :finish-reason nil}), :usage {:prompt-tokens 0, :completion-tokens 0, :total-tokens 0}}
```

 