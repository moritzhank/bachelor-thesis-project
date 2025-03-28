package de.moritzhank.cmftbl.smt.solver.dsl

data class Leq<Type>(override val lhs: Term<Type>, override val rhs: Term<Type>) :
    EvaluableRelation<Type> {
  override val type = Relation.Leq

  fun copy(): Leq<Type> = Leq(copyTerm(lhs), copyTerm(rhs))
}

data class Geq<Type>(override val lhs: Term<Type>, override val rhs: Term<Type>) :
    EvaluableRelation<Type> {
  override val type = Relation.Geq

  fun copy(): Geq<Type> = Geq(copyTerm(lhs), copyTerm(rhs))
}

data class Lt<Type>(override val lhs: Term<Type>, override val rhs: Term<Type>) :
    EvaluableRelation<Type> {
  override val type = Relation.Lt

  fun copy(): Lt<Type> = Lt(copyTerm(lhs), copyTerm(rhs))
}

data class Gt<Type>(override val lhs: Term<Type>, override val rhs: Term<Type>) :
    EvaluableRelation<Type> {
  override val type = Relation.Gt

  fun copy(): Gt<Type> = Gt(copyTerm(lhs), copyTerm(rhs))
}

data class Eq<Type>(override val lhs: Term<Type>, override val rhs: Term<Type>) :
    EvaluableRelation<Type> {
  override val type = Relation.Eq

  fun copy(): Eq<Type> = Eq(copyTerm(lhs), copyTerm(rhs))
}

data class Ne<Type>(override val lhs: Term<Type>, override val rhs: Term<Type>) :
    EvaluableRelation<Type> {
  override val type = Relation.Ne

  fun copy(): Ne<Type> = Ne(copyTerm(lhs), copyTerm(rhs))
}

enum class Relation {
  Leq,
  Geq,
  Lt,
  Gt,
  Eq,
  Ne;

  override fun toString() = when(this) {
    Leq -> "≤"
    Geq -> "≥"
    Lt -> "<"
    Gt -> ">"
    Eq -> "="
    Ne -> "≠"
  }

  fun toHTMLString() = when(this) {
    Leq -> "&le;"
    Geq -> "&ge;"
    Lt -> "&lt;"
    Gt -> "&gt;"
    Eq -> "&#61;"
    Ne -> "&ne;"
  }

}
