package com.example.foodyappkotlin.screen.login

import com.example.foodyappkotlin.data.models.User
import com.example.foodyappkotlin.data.repository.FoodyRepository
import com.example.foodyappkotlin.data.response.UserResponse
import com.example.foodyappkotlin.data.source.FoodyDataSource
import com.example.foodyappkotlin.screen.main.fragment.ODauInterface

class LoginPresenter(val repository: FoodyRepository, val view: LoginInterface.View) :
    LoginInterface.Presenter {
    override fun saveDataLogin(user: User) {
        repository.saveUserLoginData(user, object : FoodyDataSource.DataCallBack<UserResponse> {
            override fun onSuccess(user: UserResponse) {
                view.userInfoSuccess(user)
            }

            override fun onFailure(message: String) {
                view.serverFailure(message)
            }

        })
    }
}