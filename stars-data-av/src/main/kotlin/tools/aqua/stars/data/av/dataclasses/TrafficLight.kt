/*
 * Copyright 2023-2025 The STARS Project Authors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tools.aqua.stars.data.av.dataclasses

import de.moritzhank.cmftbl.smt.solver.translation.data.SmtTranslatableBase
import kotlinx.serialization.Serializable

/**
 * Data class for traffic lights.
 *
 * @property id The identifier of the traffic light.
 * @property state The current state oif the traffic light.
 * @property relatedOpenDriveId The related open drive identifier.
 * @see StaticTrafficLight
 */
@Serializable
data class TrafficLight(var id: Int, var state: TrafficLightState, val relatedOpenDriveId: Int) : SmtTranslatableBase() {
  override fun toString(): String = "TrafficLight($id, $state)"
}
