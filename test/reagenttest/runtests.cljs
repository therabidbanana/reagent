(ns reagenttest.runtests
  (:require [reagenttest.testreagent]
            [reagenttest.testcursor]
            [reagenttest.testratom]
            [reagenttest.testratomasync]
            [reagenttest.testtrack]
            [reagenttest.testwithlet]
            [reagenttest.testwrap]
            [reagenttest.performancetest]
            [reagent.impl.template-test]
            [reagent.impl.util-test]
            [clojure.test :as test]
            [doo.runner :as doo :include-macros true]
            [jx.reporter.karma :as karma]
            [reagent.core :as r]))

(enable-console-print!)

(def test-results (r/atom nil))

(def test-box-style {:position 'absolute
                     :margin-left -35
                     :color :#aaa})

(defn all-tests []
  (test/run-all-tests #"(reagenttest\.test.*|reagent\..*-test)"))

(defmethod test/report [::test/default :summary] [m]
  ;; ClojureScript 2814 doesn't return anything from run-tests
  (reset! test-results m)
  (println "\nRan" (:test m) "tests containing"
    (+ (:pass m) (:fail m) (:error m)) "assertions.")
  (println (:fail m) "failures," (:error m) "errors."))

(defn run-tests []
  (reset! test-results nil)
  (if r/is-client
    (js/setTimeout all-tests 100)
    (all-tests)))

(defn test-output-mini []
  (let [res @test-results]
    [:div
     {:style test-box-style}
     [:div {:on-click run-tests}
      (if res
        (if (zero? (+ (:fail res) (:error res)))
          "All tests ok"
          [:span "Test failure: "
           (:fail res) " failures, " (:error res) " errors."])
        "testing")]
     [:button
      {:on-click (fn [_e]
                   (reagenttest.performancetest/test-create-element))}
      "Run performance test"]]))

(defn init! []
  ;; This function is only used when running tests from the demo app.
  ;; Which is why exit-point is set manually.
  (when (some? (test/deftest empty-test))
    (doo/set-exit-point! (fn [success?] nil))
    (run-tests)
    [#'test-output-mini]))

;; Macro which sets *main-cli-fn*
(doo/doo-all-tests #"(reagenttest\.test.*|reagent\..*-test)")

(defn ^:export karma-tests [karma]
  (karma/run-all-tests karma #"(reagenttest\.test.*|reagent\..*-test)"))

(when (exists? js/window)
  (when-let [f (some-> js/window .-__karma__ .-loaded_real)]
    (.loaded_real (.-__karma__ js/window))))
