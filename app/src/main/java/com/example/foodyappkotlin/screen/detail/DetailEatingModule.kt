package com.example.foodyappkotlin.screen.detail

import android.support.v7.app.AppCompatActivity
import com.example.foodyappkotlin.common.BaseActivityModule
import com.example.foodyappkotlin.di.scope.ActivityScoped
import com.example.foodyappkotlin.di.scope.FragmentScoped
import com.example.foodyappkotlin.screen.detail.fragment_overview.OverviewFragment
import com.example.foodyappkotlin.screen.detail.fragment_overview.OverviewModule
import com.example.foodyappkotlin.screen.detail.fragment_post.PostCommentFragment
import com.example.foodyappkotlin.screen.main.fragment.ODauFragment
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [(BaseActivityModule::class)])
abstract class DetailEatingModule {

    @Binds
    @ActivityScoped
    abstract fun appCompatActivity(detailEatingActivity : DetailEatingActivity): AppCompatActivity

    @FragmentScoped
    @ContributesAndroidInjector(modules = [(OverviewModule::class)])
    abstract fun overViewFragment(): OverviewFragment

    @FragmentScoped
    @ContributesAndroidInjector
    internal abstract fun postCommentFragment(): PostCommentFragment
}

/*
    @ContributesAndroidInjector
    This is the easiest way of injecting dependency in our Activity.*/