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


## Implementation Note
1. Some ambiguous regexes, those with nested brackets, may not match with Java's pattern compilation but it matches the regex parser in https://regexr.com/
2. Only characters between these ranges are accepted in the regex: a-z, A-Z, 0-9
3. Negation is not optimized, so it might take longer, this is due to lack of time, see fluky.random (a future improvement is added as a comment)
4. This uses a manual parser of string (`string` ===lexer===> `tokens` ===parser===> `tree` + `random characters`)
5. Sample parse tree for `[-+]?[0-9]{1,16}[.][0-9]{1,6}`:

```clojure
[[:QMARK_QUANTIFIER [:SET [:CHAR \-] [:CHAR \+]]]
 [:MIN_MAX_QUANTIFIER [1 16] [:SET [:RANGE [:CHAR \0] [:CHAR \9]]]]
 [:SET [:CHAR \.]]
 [:MIN_MAX_QUANTIFIER [1 6] [:SET [:RANGE [:CHAR \0] [:CHAR \9]]]]]
```

## License

Copyright © 2020 Mourjo Sen

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
