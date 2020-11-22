# fluky

A random string generator based on a regex.

> Fluky: Something obtained or achieved more by chance than skill.

## Usage

This is a Leiningen/Clojure project, a prerequisite to compile this requires
leiningen. For the installation of Leiningen, see https://leiningen.org/

The first option is to run using shell command line arguments:
```
lein run '[-+]?[0-9]{1,16}[.][0-9]{1,6}'
"2357698.3"

lein run '[a-z]{20}'
"jkidghravucpblysumto"
```

The second option is to use the repl:
```
lein repl

fluky.core=> (random-regex "[a-z]{5}")
"ztrad"

fluky.core=> (random-regex "[-+]?[0-9]{1,16}[.][0-9]{1,6}")
"-824.7015"
```

## What is supported

- `.` Match any character except newline
- `[` Start character class definition
- `]` End character class definition
- `?` 0 or 1 quantifier
- `*` 0 or more quantifiers
- `+` 1 or more quantifier
- `{` Start min/max quantifier
- `}` End min/max quantifier

Within a character class, the following meta characters are supported:

- `^` Negate the class, but only if the first character
- `-` Indicates character range

## What is not supported

This supports only a basic grammar for regex, the following are not supported currently:
1. Character classes like `\a`
2. Possessive quantifier like `++` `*+` `*?`
3. `^` as the start of a line `^z-z`
4. `^` at anywhere outside `[]`
5. `|` Start of alternative branch
6. `(` Start subpattern
7. `)` End subpattern
8. `\1` back reference

## Implementaion

This uses a simple grammar to generate a hiccup tree using
https://github.com/Engelberg/instaparse and parses the tree to generate random strings.

Sample tree for a string `[a-z]{5}`
```
[:REGEX
 [:REGEX_CLAUSE
  [:EXACT_QUANTIFIER
   [:POS_SET "[" [:RANGE [:CHAR "a"] "-" [:CHAR "z"]] "]"]
   "{"
   [:NUMBER "5"]
   "}"]]]
```

Sample tree for `[-+]?[0-9]{1,16}[.][0-9]{1,6}`
```
[:REGEX
 [:REGEX_CLAUSE [:QMARK_QUANTIFIER [:POS_SET "[" [:CHAR "-"] [:META_CHAR "+"] "]"] "?"]]
 [:REGEX_CLAUSE
  [:MIN_MAX_QUANTIFIER
   [:POS_SET "[" [:RANGE [:CHAR "0"] "-" [:CHAR "9"]] "]"]
   "{"
   [:NUMBER "1"]
   ","
   [:NUMBER "1" "6"]
   "}"]]
 [:REGEX_CLAUSE [:POS_SET "[" [:META_CHAR "."] "]"]]
 [:REGEX_CLAUSE
  [:MIN_MAX_QUANTIFIER
   [:POS_SET "[" [:RANGE [:CHAR "0"] "-" [:CHAR "9"]] "]"]
   "{"
   [:NUMBER "1"]
   ","
   [:NUMBER "6"]
   "}"]]]
```

## License

Copyright © 2020 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
