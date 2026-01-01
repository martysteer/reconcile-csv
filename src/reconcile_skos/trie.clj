(ns reconcile-skos.trie
  "Prefix trie data structure for fast prefix matching"
  (:require [clojure.string :as str]))

;; Trie Node Structure:
;; {:char \a
;;  :concepts #{uri1 uri2 ...}  ; Concepts with labels ending at this node
;;  :children {\b node, \c node, ...}}
;;
;; Root Node:
;; {:children {\a node, \b node, ...}}

(defn create-trie
  "Create an empty trie"
  []
  {:children {}})

(defn insert-into-trie
  "Insert a label into the trie, associating it with a concept URI.
   Returns the updated trie."
  [trie label concept-uri]
  (if (str/blank? label)
    trie
    (let [chars (seq (str/lower-case label))]
      (loop [node trie
             remaining-chars chars
             path []]
        (if (empty? remaining-chars)
          ;; End of word - add concept to this node
          (assoc-in trie
                    (concat path [:concepts])
                    (conj (get-in node [:concepts] #{}) concept-uri))
          ;; Continue down the trie
          (let [ch (first remaining-chars)
                child-path (concat path [:children ch])
                child-node (get-in node [:children ch])]
            (if child-node
              ;; Child exists, continue
              (recur child-node (rest remaining-chars) child-path)
              ;; Child doesn't exist, create it
              (let [new-node {:children {}}
                    updated-trie (assoc-in trie child-path new-node)]
                (recur new-node (rest remaining-chars) child-path)))))))))

(defn build-trie
  "Build a trie from a label index.
   label-index is a map of {normalized-label -> [{:uri :type} ...]}"
  [label-index]
  (reduce
    (fn [trie [label entries]]
      (reduce
        (fn [t entry]
          (insert-into-trie t label (:uri entry)))
        trie
        entries))
    (create-trie)
    label-index))

(defn collect-all-concepts
  "Recursively collect all concept URIs from a node and its descendants"
  [node]
  (if (nil? node)
    #{}
    (let [node-concepts (get node :concepts #{})
          child-concepts (reduce
                           (fn [acc [_ child-node]]
                             (into acc (collect-all-concepts child-node)))
                           #{}
                           (:children node))]
      (into node-concepts child-concepts))))

(defn prefix-search
  "Find all concept URIs with labels starting with the given prefix.
   Returns a set of concept URIs."
  [trie prefix]
  (if (str/blank? prefix)
    ;; Empty prefix - return all concepts in trie
    (collect-all-concepts trie)
    ;; Navigate to prefix node, then collect all concepts from there
    (let [chars (seq (str/lower-case prefix))
          prefix-node (reduce
                        (fn [node ch]
                          (if node
                            (get-in node [:children ch])
                            nil))
                        trie
                        chars)]
      (if prefix-node
        (collect-all-concepts prefix-node)
        #{}))))  ; Prefix not found

(defn count-concepts
  "Count total number of concepts in the trie"
  [trie]
  (count (collect-all-concepts trie)))

(defn count-nodes
  "Count total number of nodes in the trie (for debugging)"
  [trie]
  (if (nil? trie)
    0
    (+ 1
       (reduce
         (fn [acc [_ child]]
           (+ acc (count-nodes child)))
         0
         (:children trie)))))

;; Statistics

(defn trie-stats
  "Get statistics about the trie structure"
  [trie]
  {:nodes (count-nodes trie)
   :concepts (count-concepts trie)
   :root-branches (count (:children trie))})

(defn print-trie-stats
  "Print trie statistics"
  [trie]
  (let [stats (trie-stats trie)]
    (println (format "  Trie index: %,d nodes, %,d concepts, %d root branches"
                     (:nodes stats)
                     (:concepts stats)
                     (:root-branches stats)))))
