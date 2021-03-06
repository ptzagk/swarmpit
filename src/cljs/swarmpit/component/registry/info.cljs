(ns swarmpit.component.registry.info
  (:require [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [material.icon :as icon]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.message :as message]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.progress :as progress]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form])

(defn- registry-handler
  [registry-id]
  (handler/get
    (routes/path-for-backend :registry {:id registry-id})
    {:on-success (fn [response]
                   (state/set-value response cursor))}))

(defn- delete-registry-handler
  [registry-id]
  (handler/delete
    (routes/path-for-backend :registry-delete {:id registry-id})
    {:on-success (fn [_]
                   (dispatch!
                     (routes/path-for-frontend :registry-list))
                   (message/info
                     (str "Registry " registry-id " has been removed.")))
     :on-error   (fn [response]
                   (message/error
                     (str "Registry removing failed. Reason: " (:error response))))}))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (registry-handler id))))

(rum/defc form-info < rum/static [registry]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/registries
                 (:name registry))]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:href    (routes/path-for-frontend :registry-edit {:id (:_id registry)})
          :label   "Edit"
          :primary true}))
     [:span.form-panel-delimiter]
     (comp/mui
       (comp/raised-button
         {:onTouchTap #(delete-registry-handler (:_id registry))
          :label      "Delete"}))]]
   [:div.form-view
    [:div.form-view-group
     (form/item "ID" (:_id registry))
     (form/item "NAME" (:name registry))
     (form/item "URL" (:url registry))
     (form/item "PUBLIC" (if (:public registry)
                           "yes"
                           "no"))
     (form/item "AUTHENTICATION" (if (:withAuth registry)
                                   "yes"
                                   "no"))
     (if (:withAuth registry)
       [:div
        (form/item "USERNAME" (:username registry))])]]])

(rum/defc form < rum/reactive
                 mixin-init-form
                 mixin/subscribe-form [_]
  (let [registry (state/react cursor)]
    (progress/form
      (nil? registry)
      (form-info registry))))
