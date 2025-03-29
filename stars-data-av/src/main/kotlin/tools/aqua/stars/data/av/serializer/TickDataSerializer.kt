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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import tools.aqua.stars.data.av.dataclasses.*

/** Custom [KSerializer] for [TickData]. */
object TickDataSerializer : KSerializer<TickData> {

  private val tickDataUnitSecondsSerializer by lazy { TickDataUnitSeconds.serializer() }
  private val trafficLightListSerializer by lazy { ListSerializer(TrafficLight.serializer()) }
  private val blockListSerializer by lazy { ListSerializer(Block.serializer()) }
  private val weatherParametersSerializer by lazy { WeatherParameters.serializer() }
  private val dayTimeSerializer by lazy { Daytime.serializer() }
  private val segmentSerializer by lazy { Segment.serializer() }
  private val pedestrianListSerializer by lazy { ListSerializer(Pedestrian.serializer()) }
  private val vehicleListSerializer by lazy { ListSerializer(VehicleSerializer) }

  override val descriptor: SerialDescriptor by lazy {
    buildClassSerialDescriptor("TickData") {
      element<TickDataUnitSeconds>("currentTick")
      element<List<TrafficLight>>("trafficLights")
      element<List<Block>>("blocks")
      element<WeatherParameters>("weather")
      element<Daytime>("daytime")
      element<Segment>("segment")
      element<List<Pedestrian>>("pedestrians")
      element<List<Vehicle>>("vehicles")
    }
  }


  override fun serialize(encoder: Encoder, value: TickData) {
    encoder.encodeStructure(descriptor) {
      encodeSerializableElement(descriptor, 0, tickDataUnitSecondsSerializer, value.currentTick)
      encodeSerializableElement(descriptor, 1, trafficLightListSerializer, value.trafficLights)
      encodeSerializableElement(descriptor, 2, blockListSerializer, value.blocks)
      encodeSerializableElement(descriptor, 3, weatherParametersSerializer, value.weather)
      encodeSerializableElement(descriptor, 4, dayTimeSerializer, value.daytime)
      encodeSerializableElement(descriptor, 5, segmentSerializer, value.segment)
      encodeSerializableElement(descriptor, 6, pedestrianListSerializer, value.pedestrians)
      encodeSerializableElement(descriptor, 7, vehicleListSerializer, value.vehicles)
    }
  }

  override fun deserialize(decoder: Decoder): TickData {
    error("This serializer does not support decoding.")
  }
}
