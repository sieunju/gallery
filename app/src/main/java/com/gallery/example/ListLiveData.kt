package com.gallery.example

import androidx.lifecycle.MutableLiveData

/**
 * Description : List 형태의 LiveData
 *
 * Created by juhongmin on 2023/05/14
 */
class ListLiveData<T> : MutableLiveData<List<T>>() {
    private val temp: MutableList<T> by lazy { mutableListOf() }

    init {
        value = temp
    }

    override fun getValue() = super.getValue()!!
    val size: Int get() = value.size
    operator fun get(idx: Int) =
        if (size > idx) value[idx] else throw ArrayIndexOutOfBoundsException("Index $idx Size $size")

    fun add(item: T) {
        temp.add(item)
        value = temp
    }

    fun add(index: Int, item: T) {
        temp.add(index, item)
        value = temp
    }

    fun addAll(list: List<T>) {
        temp.addAll(list)
        value = temp
    }

    fun addAll(pos: Int, list: List<T>) {
        if (pos > 0) {
            temp.addAll(pos, list)
        } else {
            temp.addAll(list)
        }
        value = temp
    }

    fun remove(item: T) {
        temp.remove(item)
        value = temp
    }

    fun removeNot(item: T) {
        temp.removeAll { it != item }
        value = temp
    }

    /**
     * 지우고 싶은 데이터 모델을 처리해주는 함수
     */
    inline fun <reified R> removeInstanceOf() {
        val tmpList = mutableListOf<T>()
        data().forEach {
            if (it !is R) {
                tmpList.add(it)
            }
        }
        data().clear()
        addAll(tmpList)
    }

    /**
     * 지우고 싶은 데이터를 true, false 로 처리하는 함수
     * @param predicate true -> 지우기, false -> Skip
     */
    fun removeIf(predicate: (T) -> Boolean) {
        val each = temp.iterator()
        while (each.hasNext()) {
            if (predicate.invoke(each.next())) {
                each.remove()
            }
        }
        value = temp
    }

    /**
     * 지우고 싶은 데이터 하나만 지우고 처리하고 리턴하는 함수
     * @param predicate true -> 지우기, false -> Skip
     */
    fun removeIfFirst(predicate: (T) -> Boolean): Boolean {
        val oldSize = temp.size
        val each = temp.iterator()
        var hasNext = each.hasNext()
        while (hasNext) {
            hasNext = if (predicate.invoke(each.next())) {
                each.remove()
                false
            } else {
                each.hasNext()
            }
        }

        // 변경된게 있다면 setValue
        return if (oldSize != temp.size) {
            value = temp
            true
        } else {
            false
        }
    }

    fun removeAll(item: List<T>) {
        temp.removeAll(item)
        value = temp
    }

    fun removeAt(index: Int) {
        temp.removeAt(index)
        value = temp
    }

    fun contains(item: T): Boolean {
        return value.contains(item)
    }

    fun data(): MutableList<T> {
        return temp
    }

    fun indexOf(item: T): Int {
        return value.indexOf(item)
    }

    fun set(idx: Int, item: T) {
        temp[idx] = item
        value = temp
    }

    fun filter(predicate: (T) -> Boolean): List<T> {
        value = temp.filter(predicate).toMutableList()
        return value
    }

    fun filtered(predicate: (T) -> Boolean): List<T> {
        val filterList = temp.filter(predicate)
        temp.clear()
        temp.addAll(filterList)
        value = temp
        return temp
    }

    fun clear() {
        temp.clear()
        value = temp
    }
}