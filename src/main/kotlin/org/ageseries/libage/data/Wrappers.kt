package org.ageseries.libage.data

class ImmutableBoolArrayView(private val array: BooleanArray) : AbstractList<Boolean>() {
    override val size: Int get() = array.size
    override fun get(index: Int) = array[index]
    override fun iterator() = array.iterator()
}

class ImmutableByteArrayView(private val array: ByteArray) : AbstractList<Byte>() {
    override val size: Int get() = array.size
    override fun get(index: Int) = array[index]
    override fun iterator() = array.iterator()
}

class ImmutableShortArrayView(private val array: ShortArray) : AbstractList<Short>() {
    override val size: Int get() = array.size
    override fun get(index: Int) = array[index]
    override fun iterator() = array.iterator()
}

class ImmutableIntArrayView(private val array: IntArray) : AbstractList<Int>() {
    override val size: Int get() = array.size
    override fun get(index: Int) = array[index]
    override fun iterator() = array.iterator()
}

class ImmutableLongArrayView(private val array: LongArray) : AbstractList<Long>() {
    override val size: Int get() = array.size
    override fun get(index: Int) = array[index]
    override fun iterator() = array.iterator()
}

class ImmutableFloatArrayView(private val array: FloatArray) : AbstractList<Float>() {
    override val size: Int get() = array.size
    override fun get(index: Int) = array[index]
    override fun iterator() = array.iterator()
}

class ImmutableDoubleArrayView(private val array: DoubleArray) : AbstractList<Double>() {
    override val size: Int get() = array.size
    override fun get(index: Int) = array[index]
    override fun iterator() = array.iterator()
}
