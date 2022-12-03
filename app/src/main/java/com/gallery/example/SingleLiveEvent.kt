package com.gallery.example

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Description :
 *
 * Created by juhongmin on 2022/11/30
 */
internal class SingleLiveEvent<T> : MutableLiveData<T>() {

    private val isPending = AtomicBoolean(false)

    /**
     * 값이 변경되면 false였던 isPending이 true로 바뀌고,
     * Observer가 호출됩니다.
     */
    @MainThread
    override fun setValue(value: T?) {
        isPending.set(true)
        super.setValue(value)
    }

    /**
     * 2. 내부에 등록된 Observer는 isPending이 true인지 확인하고,
     *    true일 경우 다시 false로 돌려 놓은 후에 이벤트가 호출되었다고 알립니다.
     */
    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(owner) {
            if (isPending.compareAndSet(true, false)) {
                observer.onChanged(it)
            }
        }
    }

    /**
     * T가 Void일 경우 호출을 편하게 하기 위해 있는 함수입니다.
     */
    @MainThread
    fun call() {
        value = null
    }
}
