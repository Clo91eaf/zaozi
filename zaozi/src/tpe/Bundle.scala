// SPDX-License-Identifier: Apache-2.0

package me.jiuyang.zaozi

import me.jiuyang.zaozi.internal.{firrtl, Context}

case class BundleField(name: String, isFlip: Boolean, tpe: Data)
abstract class Bundle extends Data:
  def Aligned[T <: Data](
    data:          T
  )(
    using valName: sourcecode.Name
  ): T =
    elements += BundleField(valName.value, false, data)
    data
  def Flipped[T <: Data](
    data:          T
  )(
    using valName: sourcecode.Name
  ): T =
    elements += BundleField(valName.value, true, data)
    data

  val elements:   collection.mutable.ArrayBuffer[BundleField] = collection.mutable.ArrayBuffer[BundleField]()
  def firrtlType: firrtl.Bundle                               =
    new firrtl.Bundle(elements.toSeq.map(bf => firrtl.BundleField(bf.name, bf.isFlip, bf.tpe.firrtlType)), false)

given [B <: Bundle, RB <: Referable[B]]: Subaccess[B, RB] with
  extension (ref: RB)
    def field[E <: Data](
      that:      String
    )(
      using ctx: Context,
      file:      sourcecode.File,
      line:      sourcecode.Line,
      valName:   sourcecode.Name
    ): Ref[E] =
      val idx       = ctx.handler.firrtlTypeGetBundleFieldIndex(ref.tpe.firrtlType.toMLIR(ctx.handler), that)
      val tpe       = ref.tpe.elements(idx).tpe
      val subaccess = ctx.handler
        .OpBuilder("firrtl.subfield", ctx.currentBlock, ctx.handler.unkLoc)
        .withNamedAttr("fieldIndex", ctx.handler.mlirIntegerAttrGet(ctx.handler.mlirIntegerTypeGet(32), idx))
        .withOperand(ref.refer)
        .withResultInference(1)
        .build()
        .results
        .head
      new Ref[E](subaccess, tpe.asInstanceOf[E])