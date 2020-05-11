package com.example.diploma.ext

import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

fun <T> Observable<T>.smartSubscribe(compositeDisposable: CompositeDisposable, consumer: (T) -> Unit) {
    compositeDisposable.add(observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe { consumer.invoke(it) })
}

fun <T> Flowable<T>?.safeSmartSubscribe(compositeDisposable: CompositeDisposable, consumer: (T) -> Unit) {
    this?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribeOn(Schedulers.computation())
            ?.subscribe { consumer.invoke(it) }?.let { compositeDisposable.add(it) }
}