(ns nightmod.core.editors
  (:require [clojure.java.io :as io]
            [compliment.core :as compliment]
            [flatland.ordered.map :as flatland]
            [nightmod.core.shortcuts :as shortcuts]
            [nightmod.core.ui :as ui]
            [nightmod.core.utils :as utils]
            [paredit.loc-utils :as loc-utils]
            [paredit.static-analysis :as static-analysis]
            [paredit.parser :as parser]
            [paredit-widget.core :as pw]
            [seesaw.color :as color]
            [seesaw.core :as s])
  (:import [java.awt.event KeyEvent KeyListener]
           [javax.swing JComponent KeyStroke]
           [javax.swing.event DocumentListener HyperlinkEvent$EventType]
           [org.fife.ui.autocomplete
            AutoCompletion BasicCompletion DefaultCompletionProvider]
           [org.fife.ui.rsyntaxtextarea
            FileLocation SyntaxConstants TextEditorPane Theme]
           [org.fife.ui.rtextarea RTextScrollPane]))

; keep track of open editors

(def editors (atom (flatland/ordered-map)))
(def font-size (atom (utils/read-pref :font-size)))
(def paredit-enabled? (atom (utils/read-pref :enable-paredit)))
(def tabs (atom nil))
(def theme-resource (io/resource "dark.xml"))
(def ^:dynamic *reorder-tabs?* true)

(defn get-text-area
  [view]
  (when view
    (->> [:<org.fife.ui.rsyntaxtextarea.TextEditorPane>]
         (s/select view)
         first)))

(defn get-text-area-from-path
  [path]
  (get-text-area (get-in @editors [path :view])))

(defn get-selected-text-area
  []
  (get-text-area-from-path (ui/get-selected-path)))

(defn get-selected-editor
  []
  (get-in @editors [(ui/get-selected-path) :view]))

(defn is-unsaved?
  [path]
  (when-let [text-area (get-text-area-from-path path)]
    (.isDirty text-area)))

; actions for editor buttons

(defn update-tabs!
  [path]
  (doto @ui/ui-root .invalidate .validate)
  (let [editor-pane (ui/get-editor-pane)]
    (when @tabs (.closeBalloon @tabs))
    (->> (for [[e-path {:keys [italicize-fn]}] (reverse @editors)]
           (format "<a href='%s' style='text-decoration: %s;
                                        font-style: %s;'>%s</a>"
                   e-path
                   (if (utils/is-parent-path? path e-path) "underline" "none")
                   (if (italicize-fn) "italic" "normal")
                   (-> e-path io/file .getName)))
         (cons "<center>PgUp PgDn</center>")
         (clojure.string/join "<br/>")
         shortcuts/wrap-hint-text
         (s/editor-pane :editable? false :content-type "text/html" :text)
         (shortcuts/create-hint! true editor-pane)
         (reset! tabs))
    (s/listen (.getContents @tabs)
              :hyperlink
              (fn [e]
                (when (= (.getEventType e) HyperlinkEvent$EventType/ACTIVATED)
                  (binding [*reorder-tabs?* false]
                    (reset! ui/selection (.getDescription e))))))
    (shortcuts/toggle-hint! @tabs @shortcuts/is-down?)))

(defn update-buttons!
  [editor ^TextEditorPane text-area]
  (ui/config! editor :#undo-button :enabled? (.canUndo text-area))
  (ui/config! editor :#redo-button :enabled? (.canRedo text-area)))

(defn save-file!
  [_]
  (when-let [text-area (get-selected-text-area)]
    (io!
      (with-open [w (io/writer (io/file (ui/get-selected-path)))]
        (.write text-area w)))
    (.setDirty text-area false)
    (s/request-focus! text-area)
    (update-buttons! (get-selected-editor) text-area))
  true)

(defn undo-file!
  [_]
  (when-let [text-area (get-selected-text-area)]
    (.undoLastAction text-area)
    (update-buttons! (get-selected-editor) text-area)))

(defn redo-file!
  [_]
  (when-let [text-area (get-selected-text-area)]
    (.redoLastAction text-area)
    (update-buttons! (get-selected-editor) text-area)))

(defn set-font-size!
  [text-area size]
  (.setFont text-area (-> text-area .getFont (.deriveFont (float size)))))

(defn set-font-sizes!
  [size & maps]
  (doseq [m maps]
    (when-let [text-area (get-text-area (:view m))]
      (set-font-size! text-area size))))

(defn save-font-size!
  [size]
  (utils/write-pref! :font-size size))

(defn decrease-font-size!
  [_]
  (swap! font-size dec))

(defn increase-font-size!
  [_]
  (swap! font-size inc))

(defn do-completion!
  [_]
  (when-let [{:keys [text-area completer] :as editor-map}
             (get @editors (ui/get-selected-path))]
    (when text-area
      (s/request-focus! text-area))
    (when completer
      (.doCompletion completer))))

(defn set-paredit!
  [enable? & maps]
  (doseq [m maps]
    (when-let [toggle-paredit-fn! (:toggle-paredit-fn! m)]
      (toggle-paredit-fn! enable?))
    (when-let [paredit-button (s/select (:view m) [:#paredit-button])]
      (s/config! paredit-button :selected? enable?))))

(defn save-paredit!
  [enable?]
  (utils/write-pref! :enable-paredit enable?))

(defn toggle-paredit!
  [_]
  (reset! paredit-enabled? (not @paredit-enabled?)))

; create and show/hide editors for each file

(def ^:const styles {"as"         SyntaxConstants/SYNTAX_STYLE_ACTIONSCRIPT
                     "asm"        SyntaxConstants/SYNTAX_STYLE_ASSEMBLER_X86
                     "bat"        SyntaxConstants/SYNTAX_STYLE_WINDOWS_BATCH
                     "c"          SyntaxConstants/SYNTAX_STYLE_C
                     "cc"         SyntaxConstants/SYNTAX_STYLE_C
                     "cl"         SyntaxConstants/SYNTAX_STYLE_LISP
                     "cpp"        SyntaxConstants/SYNTAX_STYLE_CPLUSPLUS
                     "css"        SyntaxConstants/SYNTAX_STYLE_CSS
                     "clj"        SyntaxConstants/SYNTAX_STYLE_CLOJURE
                     "cljs"       SyntaxConstants/SYNTAX_STYLE_CLOJURE
                     "cljx"       SyntaxConstants/SYNTAX_STYLE_CLOJURE
                     "cs"         SyntaxConstants/SYNTAX_STYLE_CSHARP
                     "dtd"        SyntaxConstants/SYNTAX_STYLE_DTD
                     "edn"        SyntaxConstants/SYNTAX_STYLE_CLOJURE
                     "groovy"     SyntaxConstants/SYNTAX_STYLE_GROOVY
                     "h"          SyntaxConstants/SYNTAX_STYLE_C
                     "hpp"        SyntaxConstants/SYNTAX_STYLE_CPLUSPLUS
                     "htm"        SyntaxConstants/SYNTAX_STYLE_HTML
                     "html"       SyntaxConstants/SYNTAX_STYLE_HTML
                     "java"       SyntaxConstants/SYNTAX_STYLE_JAVA
                     "js"         SyntaxConstants/SYNTAX_STYLE_JAVASCRIPT
                     "json"       SyntaxConstants/SYNTAX_STYLE_JAVASCRIPT
                     "jsp"        SyntaxConstants/SYNTAX_STYLE_JSP
                     "jspx"       SyntaxConstants/SYNTAX_STYLE_JSP
                     "lisp"       SyntaxConstants/SYNTAX_STYLE_LISP
                     "lua"        SyntaxConstants/SYNTAX_STYLE_LUA
                     "makefile"   SyntaxConstants/SYNTAX_STYLE_MAKEFILE
                     "markdown"   SyntaxConstants/SYNTAX_STYLE_NONE
                     "md"         SyntaxConstants/SYNTAX_STYLE_NONE
                     "pas"        SyntaxConstants/SYNTAX_STYLE_DELPHI
                     "properties" SyntaxConstants/SYNTAX_STYLE_PROPERTIES_FILE
                     "php"        SyntaxConstants/SYNTAX_STYLE_PHP
                     "pl"         SyntaxConstants/SYNTAX_STYLE_PERL
                     "pm"         SyntaxConstants/SYNTAX_STYLE_PERL
                     "py"         SyntaxConstants/SYNTAX_STYLE_PYTHON
                     "rb"         SyntaxConstants/SYNTAX_STYLE_RUBY
                     "s"          SyntaxConstants/SYNTAX_STYLE_ASSEMBLER_X86
                     "sbt"        SyntaxConstants/SYNTAX_STYLE_SCALA
                     "scala"      SyntaxConstants/SYNTAX_STYLE_SCALA
                     "sh"         SyntaxConstants/SYNTAX_STYLE_UNIX_SHELL
                     "sql"        SyntaxConstants/SYNTAX_STYLE_SQL
                     "tcl"        SyntaxConstants/SYNTAX_STYLE_TCL
                     "tex"        SyntaxConstants/SYNTAX_STYLE_LATEX
                     "txt"        SyntaxConstants/SYNTAX_STYLE_NONE
                     "xhtml"      SyntaxConstants/SYNTAX_STYLE_XML
                     "xml"        SyntaxConstants/SYNTAX_STYLE_XML})
(def ^:const clojure-exts #{"clj" "cljs" "cljx" "edn"})
(def ^:const wrap-exts #{"md" "txt"})
(def ^:const completer-keys #{KeyEvent/VK_ENTER
                              KeyEvent/VK_UP
                              KeyEvent/VK_DOWN})
(def ^:const console-ignore-shortcut-keys #{KeyEvent/VK_Z
                                            KeyEvent/VK_Y})

(defn get-extension
  [path]
  (->> (.lastIndexOf path ".")
       (+ 1)
       (subs path)
       clojure.string/lower-case))

(defn get-syntax-style
  [path]
  (or (get styles (get-extension path))
      SyntaxConstants/SYNTAX_STYLE_NONE))

(defn apply-settings!
  [text-area]
  ; set theme
  (-> theme-resource
      io/input-stream
      Theme/load
      (.apply text-area))
  ; set font size
  (->> (or @font-size (reset! font-size (-> text-area .getFont .getSize)))
       (set-font-size! text-area)))

(defn create-text-area
  ([]
    (doto (proxy [TextEditorPane] []
            (setMarginLineEnabled [is-enabled?]
              (proxy-super setMarginLineEnabled is-enabled?))
            (setMarginLinePosition [size]
              (proxy-super setMarginLinePosition size))
            (processKeyBinding [ks e condition pressed]
              (proxy-super processKeyBinding ks e condition pressed)))
      (.setAntiAliasingEnabled true)
      apply-settings!))
  ([path]
    (let [extension (get-extension path)]
      (doto (create-text-area)
        (.load (FileLocation/create path) nil)
        .discardAllEdits
        (.setSyntaxEditingStyle (get-syntax-style path))
        (.setLineWrap (contains? wrap-exts extension))
        (.setMarginLineEnabled true)
        (.setMarginLinePosition 80)
        (.setTabSize (if (contains? clojure-exts extension) 2 4))))))

(defn get-completion-context
  [text-area prefix]
  (let [caretpos (.getCaretPosition text-area)
        all-text (.getText text-area)
        first-str (subs all-text 0 (- caretpos (count prefix)))
        second-str (subs all-text caretpos)]
    (-> (str first-str "__prefix__" second-str)
        parser/parse
        loc-utils/parsed-root-loc
        (static-analysis/top-level-code-form caretpos)
        first
        loc-utils/node-text
        read-string
        (try (catch Exception _)))))

(defn create-completion-provider
  [text-area extension]
  (cond
    ; clojure
    (contains? clojure-exts extension)
    (proxy [DefaultCompletionProvider] []
      (getCompletions [comp]
        (let [prefix (.getAlreadyEnteredText this comp)
              context (get-completion-context text-area prefix)]
          (for [symbol-str (compliment/completions prefix context)]
            (->> (str "<html><body><pre><span style='font-size: 11px;'>"
                      (compliment/documentation symbol-str)
                      "</span></pre></body></html>")
                 (BasicCompletion. this symbol-str nil)))))
      (isValidChar [ch]
        (or (Character/isLetterOrDigit ch)
            (contains? #{\* \+ \! \- \_ \? \/ \. \: \< \>} ch))))
    ; anything else
    :else nil))

(defn create-completer
  [text-area extension]
  (when-let [provider (create-completion-provider text-area extension)]
    (doto (AutoCompletion. provider)
      (.setShowDescWindow true)
      (.setAutoCompleteSingleChoices false)
      (.setChoicesWindowSize 150 300)
      (.setDescriptionWindowSize 600 300))))

(defn install-completer!
  [text-area completer]
  (.install completer text-area)
  ; this is an ugly way of making sure paredit-widget doesn't
  ; receive the KeyEvent if the AutoComplete window is visible
  (.addKeyListener text-area
    (reify KeyListener
      (keyReleased [this e] nil)
      (keyTyped [this e] nil)
      (keyPressed [this e]
        (when (some #(.isVisible %) (.getOwnedWindows @ui/ui-root))
          (when (contains? completer-keys (.getKeyCode e))
            (let [ks (KeyStroke/getKeyStroke (.getKeyCode e) 0)
                  condition JComponent/WHEN_FOCUSED]
              (.processKeyBinding text-area ks e condition true)))
          (.consume e))))))

(defn init-paredit!
  [text-area enable-default? enable-advanced?]
  (let [toggle-paredit-fn! (pw/init-paredit! text-area enable-default?)]
    (toggle-paredit-fn! (and enable-advanced? @paredit-enabled?))
    (when enable-advanced? toggle-paredit-fn!)))

(defn is-valid-file?
  [path]
  (let [pathfile (io/file path)]
    (and (.isFile pathfile)
         (or (contains? styles (get-extension path))
             (utils/is-text-file? pathfile)))))

(defn create-editor
  [path]
  (when (is-valid-file? path)
    (let [; create the text editor object
          text-area (create-text-area path)
          extension (get-extension path)
          is-clojure? (contains? clojure-exts extension)
          completer (create-completer text-area extension)
          ; create the buttons with their actions attached
          btn-group (s/horizontal-panel
                      :items [(ui/button :id :undo-button
                                         :text (utils/get-string :undo)
                                         :focusable? false
                                         :listen [:action undo-file!])
                              (ui/button :id :redo-button
                                         :text (utils/get-string :redo)
                                         :focusable? false
                                         :listen [:action redo-file!])
                              (ui/button :id :font-dec-button
                                         :text (utils/get-string :font_dec)
                                         :focusable? false
                                         :listen [:action decrease-font-size!])
                              (ui/button :id :font-inc-button
                                         :text (utils/get-string :font_inc)
                                         :focusable? false
                                         :listen [:action increase-font-size!])
                              (ui/button :id :doc-button
                                         :text (utils/get-string :doc)
                                         :focusable? false
                                         :visible? (not (nil? completer))
                                         :listen [:action do-completion!])
                              (ui/toggle :id :paredit-button
                                         :text (utils/get-string :paredit)
                                         :focusable? false
                                         :visible? is-clojure?
                                         :selected? @paredit-enabled?
                                         :listen [:action toggle-paredit!])])
          ; create the main panel
          text-group (s/border-panel
                       :north btn-group
                       :center (RTextScrollPane. text-area))]
      ; create shortcuts
      (doto text-group
        (shortcuts/create-mappings! {:undo-button undo-file!
                                     :redo-button redo-file!
                                     :font-dec-button decrease-font-size!
                                     :font-inc-button increase-font-size!
                                     :doc-button do-completion!
                                     :paredit-button toggle-paredit!})
        shortcuts/create-hints!
        (update-buttons! text-area))
      ; update buttons every time a key is typed
      (s/listen text-area
                :key-released
                (fn [e] (update-buttons! text-group text-area)))
      ; install completer
      (when completer
        (install-completer! text-area completer))
      ; enable/disable buttons while typing
      (.addDocumentListener (.getDocument text-area)
                            (reify DocumentListener
                              (changedUpdate [this e]
                                (update-buttons! text-group text-area))
                              (insertUpdate [this e]
                                (update-buttons! text-group text-area))
                              (removeUpdate [this e]
                                (update-buttons! text-group text-area))))
      ; return a map describing the editor
      {:view text-group
       :text-area text-area
       :completer completer
       :close-fn! #(when (.isDirty text-area)
                     (save-file! nil))
       :italicize-fn #(.isDirty text-area)
       :should-remove-fn #(not (.exists (io/file path)))
       :toggle-paredit-fn! (init-paredit! text-area is-clojure? is-clojure?)})))

(defn show-editor!
  [path]
  (let [editor-pane (ui/get-editor-pane)]
    ; create new editor if necessary
    (when (and path (not (contains? @editors path)))
      (when-let [editor-map (create-editor path)]
        (swap! editors assoc path editor-map)
        (.add editor-pane (:view editor-map) path)))
    ; display the correct card
    (->> (or (when-let [editor-map (get @editors path)]
               (when *reorder-tabs?*
                 (swap! editors dissoc path)
                 (swap! editors assoc path editor-map))
               path)
             :default-card)
         (s/show-card! editor-pane))
    ; update tabs
    (update-tabs! path)
    ; give the editor focus if it exists
    (when-let [text-area (get-text-area-from-path path)]
      (s/request-focus! text-area))))

(defn remove-editors!
  [path]
  (let [editor-pane (ui/get-editor-pane)]
    (doseq [[editor-path {:keys [view close-fn! should-remove-fn]}] @editors]
      (when (or (utils/is-parent-path? path editor-path)
                (should-remove-fn))
        (swap! editors dissoc editor-path)
        (close-fn!)
        (.remove editor-pane view)))))

(defn close-selected-editor!
  []
  (let [path (ui/get-selected-path)
        file (io/file path)
        new-path (if (.isDirectory file)
                   path
                   (.getCanonicalPath (.getParentFile file)))]
    (remove-editors! path)
    (update-tabs! new-path))
  true)

; watchers

(add-watch ui/selection
           :show-editor
           (fn [_ _ _ path]
             ; remove any editors that aren't valid anymore
             (remove-editors! nil)
             ; show the selected editor
             (show-editor! path)))
(add-watch font-size
           :set-editor-font-size
           (fn [_ _ _ x]
             (apply set-font-sizes! x (vals @editors))))
(add-watch font-size
           :save-font-size
           (fn [_ _ _ x]
             (save-font-size! x)))
(add-watch paredit-enabled?
           :set-editor-paredit
           (fn [_ _ _ enable?]
             (apply set-paredit! enable? (vals @editors))))
(add-watch paredit-enabled?
           :save-paredit
           (fn [_ _ _ enable?]
             (save-paredit! enable?)))
