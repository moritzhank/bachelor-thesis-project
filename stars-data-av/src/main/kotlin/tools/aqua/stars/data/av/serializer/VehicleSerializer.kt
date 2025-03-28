/*
 * Copyright 2024-2025 The STARS Project Authors
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

package tools.aqua.stars.data.av.serializer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import tools.aqua.stars.data.av.dataclasses.*
import de.moritzhank.cmftbl.smt.solver.translation.data.encoding.buildSafeSerialDescriptor

/** Custom [KSerializer] for [Vehicle]. */

object VehicleSerializer : KSerializer<Vehicle> {

  private val tickDataSerializer by lazy { TickDataSerializer }
  private val laneSerializer by lazy { Lane.serializer() }
  private val locationSerializer by lazy { Location.serializer() }
  private val vector3DSerializer by lazy { Vector3D.serializer() }
  private val rotationSerializer by lazy { Rotation.serializer() }

  @OptIn(ExperimentalSerializationApi::class)
  override val descriptor: SerialDescriptor by lazy {
    buildSafeSerialDescriptor(Vehicle::class) {
      if (it == TickData.javaClass) TickDataSerializer else null
    }
  }

  override fun serialize(encoder: Encoder, value: Vehicle) {
    encoder.encodeStructure(descriptor) {
      encodeIntElement(descriptor, 0, value.id)
      encodeSerializableElement(descriptor, 1, tickDataSerializer, value.tickData)
      encodeDoubleElement(descriptor, 2, value.positionOnLane)
      encodeSerializableElement(descriptor, 3, laneSerializer, value.lane)
      encodeStringElement(descriptor, 4, value.typeId)
      encodeBooleanElement(descriptor, 5, value.isEgo)
      encodeSerializableElement(descriptor, 6, locationSerializer, value.location)
      encodeSerializableElement(descriptor, 7, vector3DSerializer, value.forwardVector)
      encodeSerializableElement(descriptor, 8, rotationSerializer, value.rotation)
      encodeSerializableElement(descriptor, 9, vector3DSerializer, value.velocity)
      encodeSerializableElement(descriptor, 10, vector3DSerializer, value.acceleration)
      encodeSerializableElement(descriptor, 11, vector3DSerializer, value.angularVelocity)
      encodeDoubleElement(descriptor, 12, value.effVelocityInKmPH)
    }
  }

  override fun deserialize(decoder: Decoder): Vehicle {
    error("This serializer does not support decoding.")
  }
}
