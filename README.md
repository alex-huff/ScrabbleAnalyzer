# ScrabbleAnalyzer
ScrabbleAnalyzer is a Scrabble/Words With Friends analyzer.
# In action
https://user-images.githubusercontent.com/38389408/222550305-b2dd3ddf-c924-4d09-baba-58d40bd728cf.mp4
# About
## Technologies
ScrabbleAnalyzer is a Java Swing application. It is written purely in Java. Because of this, it is cross-platform, meaning it can run on Windows, MacOS, and GNU/Linux or any other ecosystem that has a compatible JVM implementation.
- Java >= 17
- [Apache Batik](https://xmlgraphics.apache.org/batik/) (For rasterizing SVGs)
## Running
- Download the [latest release](https://github.com/alex-huff/ScrabbleAnalyzer/releases)
- Run the file:
  - From command line: 
    - ```shell
      $ java -jar ScrabbleAnalyzer.jar
      ```
  - From file explorer:
    - Just double-click the file assuming your environment has a valid association for .jar files.
## How to use ScrabbleAnalyzer
To use ScrabbleAnalyzer, you first need to input the current state of your game into the board. For each played word, click on the tile at the start of the word. Then, type out the word, holding down shift if the word is vertical.

![wordPlace](https://user-images.githubusercontent.com/38389408/222557471-cb2053f9-d8ff-4698-bb45-be5758d544bd.gif)

If one or more of the tiles were placed using a wildcard tile, they do not count towards the score, and you need to indicate that to the program. To do so, just right-click on the tile to remove the points.

![wild](https://user-images.githubusercontent.com/38389408/222560334-f1b3c0a1-4728-4172-9e2b-1c9197f62687.gif)

Once all the words have been placed on the board, input your current tiles into the tile rack. Use '0' to represent a wildcard tile.

![rack](https://user-images.githubusercontent.com/38389408/222562407-931a5086-bdd1-4230-b20a-9e3df8f0173e.gif)

The list view should then populate with every single playable word, sorted by score. Any change to the board or to the rack will automatically trigger a recalculation of playable moves, and the list will update accordingly.

Selecting on a list item will preview the move, and pressing enter will then play that move. This will update your tile rack as well. Any action can be undone, and redone using the keybinds Ctrl+Z, and Ctrl+R.

Games can be saved to, and loaded from a file.

![play](https://user-images.githubusercontent.com/38389408/222566115-55b830f0-98e3-4752-8306-f85fd22e1446.gif)

You can select between two tile styles: Isometric, and Flat.

<p align="middle">
  <img src="https://raw.githubusercontent.com/alex-huff/ScrabbleAnalyzer/d1bbc7fe60ecc78dfc072579e994af30fb5aa614/src/main/resources/assets/tile/isonormZ.svg" alt="Isometric" width="40%">
  <img src="https://raw.githubusercontent.com/alex-huff/ScrabbleAnalyzer/d1bbc7fe60ecc78dfc072579e994af30fb5aa614/src/main/resources/assets/tile/flatnormZ.svg" alt="Flat" width="40%">
</p>

## How it works
Under the hood, ScrabbleAnalyzer makes use of the [Directed Acyclic Word Graph (DAWG)](https://en.wikipedia.org/wiki/Deterministic_acyclic_finite_state_automaton) data-structure. DAWGs have capabilities that enable ScrabbleAnalyzer's algorithm to perform at high speeds. Namely, when using a DAWG to represent a dictionary of words, DAWGs can calculate if a word is in the dictionary, or if a prefix is in the dictionary, in a speed proportional to the word/prefix length. ScrabbleAnalyzer's algorithm traverses a tree representing the permutations of tiles and the DAWG at the same time, culling whole paths based on whether the currently formed word is a valid prefix in the dictionary. This cuts out whole swaths of tile permutations that a more naive, bruteforce algorithm would needlessly iterate through.

Another source of optimization comes from an initial sweep of the board that yields pre-computed scoring data, and a list of valid word placement tiles that is drastically culled from the initial 15x15 board. Extra information is also stored with the valid word placement tiles that help to cull iterations based on word length, and perpendicularly intersecting words.

Together, these optimizations enable ScrabbleAnalyzer to compute, and sort, every possible move in just a few milliseconds.
