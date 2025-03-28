package de.moritzhank.cmftbl.smt.solver.experiments

/*
val changedLane =
          predicate(Vehicle::class) { _, v ->
            eventually(v) { v0 ->
              eventually(v0) { v1 -> v0.lane.road == v1.lane.road && v0.lane != v1.lane }
            }
          }

          ∃v.◊(velocity(v)=0∧◊(velocity(v)=0))

 */

/*
val hasMidTrafficDensity = formula { v: CCB<Vehicle> ->
  registerFunction(TickData::vehiclesInBlock, vehiclesInBlock)
  minPrevalence(0.6) {
    val block = v * Vehicle::lane * Lane::road * Road::block
    val numVehicles =
      term(
        (v * Vehicle::tickData * TickData::vehiclesInBlock).withParam(block) *
                List<Vehicle>::size)
    const(6) leq numVehicles and (numVehicles leq const(15))
  }
}
*/
