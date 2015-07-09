# Relational System Z Reasoner

## Functionality

This is a prototype implementation of an approach for default reasoning with first-order conditionals, similar to System Z by Judea Pearl. It provides data structures and algorithms to compute ranking functions for first-order knowledge bases and to model the induced inference relations. A simple GUI is available to parse knowledge bases and work with them conveniently.

## Usage

To start the program, download the jar-file in *dist* and run it:

```java -jar RelationalSystemZ.jar```

## Background

The program was created as part of the author's master thesis, which can be found in the top folder. More background on the topic as well as an detailed explanation of the program can be found in it. 

The implemented approach was first published by Kern-Isberner and Beierle:

> G. Kern-Isberner and C. Beierle. **A system Z-like approach for first-order default reasoning**. In Thomas Eiter, Hannes Strass, Miroslaw Truszczynski, and Stefan Woltran, editors, *Advances in Knowledge Representation, Logic Programming, and Abstract Argumentation - Essays Dedicated to Gerhard Brewka on the Occasion of His 60th Birthday*, volume 9060 of LNAI, pages 81-95. Springer, 2015.

## Libraries

The implementation is based on the *Log4KR* library that provides data structures and parser for propositional and first-order logic.

[https://www.fernuni-hagen.de/wbs/research/log4kr/index.html](https://www.fernuni-hagen.de/wbs/research/log4kr/index.html)

It is a sub project of the *KReator* project:

[http://kreator-ide.sourceforge.net/](http://kreator-ide.sourceforge.net/)