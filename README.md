# draconic.ui

A Clojure library intended to make developing similar applications for multiple platforms relitively simple. This is *not* a cross-platform ui toolkit. The philosophy behind this library is that each platform has its own set of guidelines, best practices, and idioms that users of the platform will expect. The job of a professional application developer is to ensure that their presence on each target adheres to these standards. It is wholly inappropriate to present users of a platform with a product that doesn't respect their habits and expectations, even if the technology behind them is similar or identical (ie, uses Clojure/react/JavaFX/etc). Effort should be made to ensure that a project never need suffer in this way for a lack of development resources. This library is a step in that direction.

The library is designed around the "Atomic Node" abstraction. In essence, an Atomic Node is a single irreducible UI element that contains state, has semantics related to that state that do not change between platforms (are constant with the data), and that can be reasoned about without knowing anything about which platform the nodes will eventually be from. Please see atomic-node.md in the docs folder for more information. 

## Usage


[![Clojars Project](https://img.shields.io/clojars/v/draconic.ui.svg)](https://clojars.org/draconic.ui)


An application using draconic.ui will be structured as at least one library project devoted to dealing with the data model, and one or more platform-specific application projects that depend on it and a platform-specific implementation of the Atomic Node abstraction. The data library will use the Atomic Node abstraction to make one or more functions that bind ui nodes to data and behavior (effectively platform-independent behaviors), and the application projects will invoke these functions with the appropriate arguments.

## License

Copyright © 2017 Alexander Chythlook

Distributed under the Eclipse Public License either version 1.0
