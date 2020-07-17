---
name: Feature request
about: Suggest an new lint or migration rule
title: ''
labels: enhancement
assignees: ''

---

**Description**

Explain the rationale of the rule. Ex: "Refactor the BSONDocument import"

**Please provide the original code**
Scala code before the requested rule is applied. The code can be compiled without error in Scala REPL with ReactiveMongo dependencies (no tier dependency required). Ex:

```scala
import reactivemongo.bson.BSONDocument
```

Also indicate the versioned ReactiveMongo dependencies for SBT build.Ex:

```ocaml
"org.reactivemongo" %% "reactivemongo-bson" % "0.18.6"
```

**Code after the request rule**
Scala code after the requested rule is applied. The code can be compiled without error in Scala REPL with ReactiveMongo dependencies (no tier dependency required). Ex:

```scala
import reactivemongo.api.bson.BSONDocument
```

Also indicate the versioned ReactiveMongo dependencies for SBT build.Ex:

```ocaml
"org.reactivemongo" %% "reactivemongo-bson-api" % "0.18.6"
```
