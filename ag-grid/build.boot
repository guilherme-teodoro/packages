(set-env!
  :resource-paths #{"resources"}
  :dependencies '[[cljsjs/boot-cljsjs "0.8.2" :scope "test"]])

(require '[cljsjs.boot-cljsjs.packaging :refer :all]
         '[boot.core :as boot]
         '[boot.tmpdir :as tmpdir]
         '[boot.util :refer [dosh]]
         '[clojure.java.io :as io])

(def +lib-version+ "13.0.1")
(def +lib-checksum+ "42138AC6CF0B64E36EBE6A89EA8AB284")
(def +version+ (str +lib-version+ "-0"))
(def +lib-folder+ (format "ag-grid-%s" +lib-version+))

(defn- dosh-cmd [& args]
       (apply dosh (if (re-find #"^Windows" (.get (System/getProperties) "os.name"))
                     (into ["cmd.exe" "/c"] args)
                     args)))

(defn- path [x]
       (.toString (java.nio.file.Paths/get x (into-array String nil))))

(task-options!
  pom {:project     'cljsjs/ag-grid
       :version     +version+
       :description "The JavaScript Datagrid for Enterprise"
       :url         "http://ag-grid.com/"
       :scm         {:url "https://github.com/cljsjs/packages"}
       :license     {"MIT" "http://opensource.org/licenses/MIT"}})

(deftask download-lib []
         (download :url (format "https://github.com/ceolter/ag-grid/archive/%s.zip" +lib-version+)
                   :checksum +lib-checksum+
                   :unzip true))

(deftask build []
         (let [tmp (boot/tmp-dir!)]
              (with-pre-wrap fileset
                             (doseq [f (boot/input-files fileset)
                                     :let [target (io/file tmp (tmpdir/path f))]]
                                    (io/make-parents target)
                                    (io/copy (tmpdir/file f) target))
                             (binding [boot.util/*sh-dir* (str (io/file tmp +lib-folder+))]
                                      (dosh-cmd "npm" "install")
                                      (dosh-cmd "npm" "install" "gulp" "bower")
                                      (dosh-cmd (path (str (io/file tmp +lib-folder+) "/node_modules/.bin/bower")) "install")
                                      (dosh-cmd (path (str (io/file tmp +lib-folder+) "/node_modules/.bin/gulp")) "webpack-all"))
                             (-> fileset (boot/add-resource tmp) boot/commit!))))

(deftask package []
         (comp
           (download-lib)
           (build)
           (sift :move {#".*ag-grid.js"                      "cljsjs/ag-grid/development/ag-grid.inc.js"
                        #".*dist/styles/ag-grid.css"         "cljsjs/ag-grid/development/ag-grid.inc.css"
                        #".*dist/styles/theme-blue.css"      "cljsjs/ag-grid/development/theme-blue.css"
                        #".*dist/styles/theme-bootstrap.css" "cljsjs/ag-grid/development/theme-bootstrap.inc.css"
                        #".*dist/styles/theme-dark.css"      "cljsjs/ag-grid/development/theme-dark.inc.css"
                        #".*dist/styles/theme-fresh.css"     "cljsjs/ag-grid/development/theme-fresh.inc.css"
                        #".*dist/styles/theme-material.css"  "cljsjs/ag-grid/development/theme-material.inc.css"})
           (sift :include #{#"^cljsjs"})
           (minify :in "cljsjs/ag-grid/development/ag-grid.inc.js"
                   :out "cljsjs/ag-grid/production/ag-grid.min.inc.js"
                   :lang :ecmascript5)

           (minify :in "cljsjs/ag-grid/development/ag-grid.inc.css"
                   :out "cljsjs/ag-grid/production/ag-grid.min.inc.css")
           (minify :in "cljsjs/ag-grid/development/theme-blue.css"
                   :out "cljsjs/ag-grid/production/theme-blue.min.css")
           (minify :in "cljsjs/ag-grid/development/theme-bootstrap.inc.css"
                   :out "cljsjs/ag-grid/production/theme-bootstrap.min.inc.css")
           (minify :in "cljsjs/ag-grid/development/theme-dark.inc.css"
                   :out "cljsjs/ag-grid/production/theme-dark.min.inc.css")
           (minify :in "cljsjs/ag-grid/development/theme-fresh.inc.css"
                   :out "cljsjs/ag-grid/production/theme-fresh.min.inc.css")
           (minify :in "cljsjs/ag-grid/development/theme-material.inc.css"
                   :out "cljsjs/ag-grid/production/theme-material.mininc.css")
           (deps-cljs :name "cljsjs.ag-grid")
           (pom)
           (jar)))
