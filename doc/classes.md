# Classes

## Overview and Definition
In OOP, a class is a technical term that means "template for an object". It inarguably derives this from the formal definition of the word "class":

    http://www.dictionary.com/browse/class

    Class

    noun

    a number of persons or things regarded as forming a group by
    reason of common attributes, characteristics, qualities, or traits; kind; sort:
    *a class of objects used in daily living.*
    
In clojure, there are no technical classes. If we want to talk about a java class, it is common practice to simply say "Java Class". This leaves the word "class" itself free to be used in other ways. For draconic.ui, the term "class" is used thusly:

     Class
     
     noun
     
     A group of ui nodes that have similar behavior, requirements, and
     semantics in terms of what data they consume and produce.
     
For this library, a node in the text class has a string as its :state, displays this string to the user, and fires a :state-change event that contains two strings. Nodes in this class might also be part of the option class, which present the user with two or more options to choose from that may or may not accept user-provided options as input. Each node has a Set of class-denoting keywords (namespace-qualified to draconic.ui) that denote which class they belong to, under the :class key in their :attribute map.

Having this notion of classes is important primarily because it allows generic ui binding code to be written without knowing exactly what kind of node it will be used on. If I want to get someone's name, for example, I know that I need a :text class node that is also :user-editable class. If I want a user to select from an exclusive list of options, I know that I need an :option class node (that is also either :text class, or :arbitrary class if I want to use Joda dates, for example). This then allows an app developer to try out different approaches. By not tying this notion of "class" to a specific technological implementation, this frees up platform users to define collections of objects or raw data itself as members of a given class (which should come in handy for Reagent bindings, among other things). An additional benefit is that other classes can be added later, extending the notion should it be needed for domain-specific purposes.

## Default Classes

| Class      | :state type                 | Extra information under                      |   |   |
|------------|-----------------------------|----------------------------------------------|---|---|
| :text      | String                      | :user-editable?                      |   |   |
| :number    | Any number, Defaults to Int | :type, :user-editable?              |   |   |
| :switch    | boolean                     |                                              |   |   |
| :option    | Any, Defaults to String     | :options, :type, :render-fn, :user-editable? |   |   |
| :container | Coll of nodes under it in a scene graph.        |                                              |   |   |
### :text
A class that displays text to the user. If :user-editable? is false on a node that is able to handle user input, that functionality will be disabled. :user-editable? is always false for non-editable labels, etc.

### :number
Displays a number to the user. Default :type is :int, additional options include :float, :hex. (Other types like Long and Double are to be coerced if necessary.)

### :switch
Displays a node that is either on or off. Note that this behavior could also be simulated with an :option node of type :boolean and :options [true false], as could a number widget be made out of an :option node of type :int and options (range 0 999999999). 

### :option
Allows the user to select from a set of options. Can be combined with other classes if the node *always* follows that behavior (a rating widget would also be :number but a drop-down would not, etc). :type defaults to :string, but can be anything acceptable. :render-fn is a function of one arg that takes an item from :options and returns a String, defaults to #(str %). If :user-editable? evals true (defaults false), any arbitrary input can be passed in as an alternative.

:options is a seq that will be used to populate the node's list of options, ie the auto-complete of a text box, the items displayed by a spinner, the labeled radio buttons in a group, or the list of possible selections in a drop-down menu. Can also be a (range), esp if the node is a :number class. As the node usually needs to consume the entire seq before it can be used, infinite seqs should be avoided. Defaults to [1 2 3].

### :container
:container type nodes should be used by implementing applications only. Layout details, as well as organizational information, are best left out of the back-end data logic in order to reduce complexity and eliminate the chance of one target platform holding another back.