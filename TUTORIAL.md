## Getting Started

After downloading the jar file from [the website](https://nightmod.net/) and running it, you'll see a screen listing several different types of games. Let's start off with the simplest one, Barebones. After clicking it and giving it a name, you should see a screen saying "Hello world!" at the bottom. Now click the button at the top that looks like a grid, and you'll see a file grid appear that contains "core.clj". Click that to open the file in the built-in editor.

## Game Structure

In `defscreen`, you'll find that a few simple functions are defined: `:on-show` and `:on-render`. The first only runs when the screen is first shown, and the second is run every single time your game wants to draw on the screen (which is ideally 60 times per second).

There are many other functions you can put inside `defscreen`, each letting you run code when certain important events happen. For now, we'll stick to the two we started with, because they are the most fundamental, but you can read [the documentation](http://oakes.github.io/play-clj/core.defscreen.html) to learn about the others.

## Entity System

Most games need some way to keep track of all the things displayed within them. We call these things "entities". Normally, we need to remember attributes such as their position, size, and possibly other values like health and damage. In play-clj, these entities are simply maps, so you can store whatever you want inside of them.

Often, games will store these entities in a list, and in their render function they will loop over the list, modify the entities (such as moving them), and then call a function to render them. With functional programming, on the other hand, we want to avoid directly mutating values, as it leads to more complicated and error-prone software.

Instead, play-clj stores the entities vector behind the scenes and passes it to each function within `defscreen`. It's a normal Clojure vector, so you can't directly change it. Instead, you must return a new entities vector at the end of each `defscreen` function, which will then be provided to all other functions when they run.

## Loading a Texture

Now let's find an image to use as a texture in the game. Find one you'd like to use, such as [this Clojure logo](http://upload.wikimedia.org/wikipedia/en/1/1d/Clojure_logo.gif), and save it to your project's folder. You can find this folder by going back to the file grid (click the ↑ arrow button) and clicking "Open in File Browser", which should bring up the correct folder so you can copy the image into it. If you succeeded, you'll see it appear in the file grid, and you can go back to the "core.clj" file.

Next, simply change the line where the label entity is being created, so it creates a texture from that file instead:

```clojure
    (texture "Clojure_logo.gif")
```

## Size and Position

If you run the code now, you'll see the image in the bottom-left corner. As mentioned, entities such as the one created by `texture` are simply Clojure records. By default, our entity will look like this:

```clojure
#play_clj.entities.TextureEntity{:object #<TextureRegion com.badlogic.gdx.graphics.g2d.TextureRegion@4634b96>}
```

A `texture` contains the underlying Java object. By default, it will be drawn at the bottom-left corner with the size of the image itself. You can change the position and size by simply using `assoc`:

```clojure
    (assoc (texture "Clojure_logo.gif")
           :x 50 :y 50 :width 100 :height 100)
```

You can also set scaling and rotation on a texture using :scale-x, :scale-y, and :angle, which all use (:origin-x, :origin-y) as the center. For example, here we rotate it 45 degrees counter-clockwise around the bottom-left corner:

```clojure
    (assoc (texture "Clojure_logo.gif")
           :x 50 :y 50 :width 100 :height 100
           :angle 45 :origin-x 0 :origin-y 0)
```

## Input

Let's add a new function at the end of `defscreen` called `:on-key-down`, which runs when a key is pressed:

```clojure
  :on-key-down
  (fn [screen entities]
    )
```

If takes the same form as the other functions, expecting a new entities vector to be returned at the end. The first argument, `screen`, which we haven't talked about yet, is a Clojure map containing various important values. In the `:on-key-down` function, it will contain a `:key` which indicates what key was pressed.

To figure out what key it refers to, you'll need to compare it to a known key code, which you can get with `key-code`. See [the documentation](http://oakes.github.io/play-clj/core.key-code.html) or the example below to learn how to use it.

Let's write a conditional statement that prints out which arrow key you pressed. Note that if a `defscreen` function returns `nil`, it leaves the entities vector unchanged, so the code below won't wipe out the entities vector.

```clojure
  :on-key-down
  (fn [screen entities]
    (cond
      (= (:key screen) (key-code :dpad-up))
      (println "up")
      (= (:key screen) (key-code :dpad-down))
      (println "down")
      (= (:key screen) (key-code :dpad-right))
      (println "right")
      (= (:key screen) (key-code :dpad-left))
      (println "left")))
```

Now, what about mobile devices? We may not have a keyboard, so let's create an `:on-touch-down` function:

```clojure
  :on-touch-down
  (fn [screen entities]
    )
```

The [game](http://oakes.github.io/play-clj/core.game.html) function gives you convenient access to the window dimensions and the x/y position of the pointer:

```clojure
  :on-touch-down
  (fn [screen entities]
    (cond
      (> (game :y) (* (game :height) (/ 2 3)))
      (println "up")
      (< (game :y) (/ (game :height) 3))
      (println "down")
      (> (game :x) (* (game :width) (/ 2 3)))
      (println "right")
      (< (game :x) (/ (game :width) 3))
      (println "left")))
```

Conveniently, the `:on-touch-down` function also runs when a mouse is clicked on the screen, so we are adding mouse support to the game as well!

## Movement

We already know how to change an entity's position, so let's leverage that to make our image move when we hit the keys. Make a new function above `defscreen` that takes the entity and a keyword, and returns the entity with an updated position:

```clojure
(defn move
  [entity direction]
  (case direction
    :down (assoc entity :y (dec (:y entity)))
    :up (assoc entity :y (inc (:y entity)))
    :right (assoc entity :x (inc (:x entity)))
    :left (assoc entity :x (dec (:x entity)))
    nil))
```

Now we can update our `:on-key-down` and `:on-touch-down` functions to move the entity. Note that we are technically returning a single entity rather than an entities vector, but play-clj will turn it back into a vector automatically.

```clojure
  :on-key-down
  (fn [screen entities]
    (cond
      (= (:key screen) (key-code :dpad-up))
      (move (first entities) :up)
      (= (:key screen) (key-code :dpad-down))
      (move (first entities) :down)
      (= (:key screen) (key-code :dpad-right))
      (move (first entities) :right)
      (= (:key screen) (key-code :dpad-left))
      (move (first entities) :left)))
      
  :on-touch-down
  (fn [screen entities]
    (cond
      (> (game :y) (* (game :height) (/ 2 3)))
      (move (first entities) :up)
      (< (game :y) (/ (game :height) 3))
      (move (first entities) :down)
      (> (game :x) (* (game :width) (/ 2 3)))
      (move (first entities) :right)
      (< (game :x) (/ (game :width) 3))
      (move (first entities) :left)))
```

## Camera

To make your game adjust its ratio for different screen sizes, you need to use a camera. Your game should already create one in the `:on-show` function, like this:

```clojure
    (update! screen :renderer (stage) :camera (orthographic))
````

Orthographic cameras are for 2D games, so that's what we're using. We also have a function called `:on-resize`, which will run whenever the screen resizes:

```clojure
  :on-resize
  (fn [screen entities]
    (height! screen (:height screen)))
```

The `height!` function is telling the camera to set its height to the new height of the screen, and automatically adjust its width so it matches the ratio of the screen itself. Try to temporarily change `(:height screen)` to an arbitrary number, such as 300. The image will now look bigger, because the camera's viewport is now smaller.

## Timers

It is often necessary to do something in the future or at regular intervals. For this, we have `add-timer!` and its companion, `remove-timer!`. Let's suppose you want to spawn a new enemy exactly 10 seconds after the game begins. First, add the timer in the beginning of the `:on-show` function:

```clojure
    (add-timer! screen :spawn-enemy 10)
```

Then, add the `:on-timer` function to your screen:

```clojure
  :on-timer
  (fn [screen entities]
    )
```

If you want `:on-timer` to run at a regular 2-second interval, just add that as an argument:

```clojure
    (add-timer! screen :spawn-enemy 10 2)
```

If you want it to run exactly 20 times, you can add one final argument to specify how many times it should repeat after the first run:

```clojure
    (add-timer! screen :spawn-enemy 10 2 19)
```

Of course, you can add more than one timer. The id you supply them will be supplied to the `:on-timer` function's screen map:

```clojure
  :on-timer
  (fn [screen entities]
    (case (:id screen)
      :spawn-enemy (conj entities (create-enemy))
      :spawn-friend (conj entities (create-friend))
      nil))
```

Lastly, at any time you can remove a timer:

```clojure
    (remove-timer! screen :spawn-enemy)
```

## Java Interop

At some point, you will need to do more than simple positioning and sizing. For that, you'll need to call libGDX methods directly. You could, of course, use Clojure's [Java interop](http://clojure.org/java_interop) syntax on the `:object` contained within the entity. This is a bit ugly, though, and requires you to do all the importing and type hinting yourself.

In play-clj, many different calls, such as `texture`, are actually macros that allow you to call the underlying Java methods after the required argument(s). In this case, the underlying class is called [TextureRegion](http://libgdx.badlogicgames.com/nightlies/docs/api/com/badlogic/gdx/graphics/g2d/TextureRegion.html). Consider this:

```clojure
    (texture "Clojure_logo.gif" :flip true false)
```

...which is transformed into:

```clojure
    (let [entity (texture "Clojure_logo.gif")]
      (doto ^TextureRegion (:object entity)
        (.flip true false))
      entity)
```

You can even call multiple methods in the same expression this way. For example:

```clojure
    (texture "Clojure_logo.gif"
             :flip true false
             :set-region 0 0 100 100)
```

...which is transformed into:

```clojure
    (let [entity (texture "Clojure_logo.gif")]
      (doto ^TextureRegion (:object entity)
        (.flip true false)
        (.setRegion 0 0 100 100)
      entity)
```

There is also an equivalent macro with a `!` on the end, which lets you call these methods on an existing entity:

```clojure
    (texture! entity :flip true false)
```

In this case, you can only include a single method call, because it's also meant to be a simple way to call getter methods that return a value. For example:

```clojure
    (texture! entity :get-region-width)
```

Lastly, there is one final version with a `*` at the end. Essentially, `texture*` is the function version of `texture`. It has the same required arguments, but it can't do any of the Java interop stuff noted above. This is useful because sometimes you may want to pass `texture` around as a function, such as in the first argument of `map`.

If you try that, you'll get the dreaded error: `java.lang.RuntimeException: Can't take value of a macro`. Macros run at compile time, so you can't pass them around like functions. The solution is to use the `*` version, which is indeed a function:

```clojure
    (map texture* ["image1.png" "image2.png" "image3.png"])
```

## Multiple Screens

It is possible to have multiple screens for your game. You may want a title screen at first, and then go to your game when an item is clicked. You can do this by simply calling the same `set-game-screen!` function that is run at the beginning of the game. You'll need to declare your symbols at the top of your file, so you can refer to them from anywhere below.

`(declare title-screen main-screen)`

You may want to display two different screens at once. This is useful in situations where you want to overlay something on your game that you want to remain fixed and independent of the game's camera. For example, to display a label with the current frames per second, create another screen like this:

```clojure
(defscreen text-screen
  :on-show
  (fn [screen entities]
    (update! screen :camera (orthographic) :renderer (stage))
    (assoc (label "0" (color :white))
           :id :fps
           :x 5))
           
  :on-render
  (fn [screen entities]
    (->> (for [entity entities]
           (case (:id entity)
             :fps (doto entity (label! :set-text (str (game :fps))))
             entity))
         (render! screen)))
         
  :on-resize
  (fn [screen entities]
    (height! screen 300)))
```

Then, at the end of the file, set the screens in the order in which you'd like them to appear:

```clojure
(set-game-screen! main-screen text-screen)
```

Note that only the first screen, which in this case is `main-screen`, calls `(clear!)` in its `:on-render` function. If `text-screen` called it as well, it would clear whatever was drawn by `main-screen`.

With multiple screens being displayed, it will often be important to make them interact. For example, you may want to place a button on `text-screen` that causes a character on `main-screen` to change color. You can do this with the [screen!](http://oakes.github.io/play-clj/core.screen!.html) function.

First, define a custom screen function in `main-screen` with a name such as `:on-change-color`, where you can write the code that changes the character's color. Then, in `text-screen`, listen for the button click (using the `:on-ui-changed` screen function) and manually run the custom function. You may optionally provide key-value pairs that will be passed into its screen map:

```clojure
(screen! main-screen :on-change-color :color :blue)
```

## Using the REPL

The REPL view can be displayed by clicking the > button. This is particularly useful for reading and modifying state. We'll be using some REPL-specific functions, so type `(use 'play-clj.repl)` to bring them in. Let's peek into the entities vector by typing the following into the REPL:

`(e main-screen)`

That should print out a vector with a single map inside of it. Now try moving your image and then run the command again. The `:x` and `:y` values should now be updated. You're looking at your game in real-time! Lastly, let's try moving the entity from the REPL:

`(e! identity main-screen :x 200 :y 200)`

## Building a Standalone Game

Follow these steps to build your game as a standalone jar file:

* Create a [play-clj](https://github.com/oakes/play-clj) game. To do that, download [Nightcode](https://nightcode.info/), click "New Project", provide a name, and choose the "Game" option.
* Copy all the files from your Nightmod game to the `desktop/resources` folder in your Nightcode project. If you don't know where your Nightmod game files are stored, you can find them by clicking the "Open in File Browser" button in Nightmod's file pane.
* Replace `desktop/src-common/.../core.clj` with [this one](https://gist.github.com/oakes/20e11ede32df6168e6ed).
* In your new core.clj file, replace every instance of "my-game" with whatever name you provided to Nightcode.
* You can try it out by clicking "Run" in Nightcode, and build a jar file with "Build".
