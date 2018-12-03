package com.developer.java.yandex.vkwall

import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.developer.java.yandex.vkwall.R.id.*
import com.developer.java.yandex.vkwall.adapter.VkWallAdapter
import com.developer.java.yandex.vkwall.entity.VkWallEntity
import com.developer.java.yandex.vkwall.model.WallModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        public val token = "a10b7fd7a10b7fd7a10b7fd715a144c62baa10ba10b7fd7fafe18ad246c4fcef5af6a9f"
        private const val LIST_ITEM_KEY: String = "listItemKey"
        private const val TV_ID_TEXT: String = "textViewIdText"
        private const val TV_COUNT_TEXT: String = "textViewCountText"
        private const val POSITION_SCROLL_VIEW: String = "positionScrollView"
    }

    private lateinit var mAdapter: VkWallAdapter
    private lateinit var mModel: WallModel
    private val disposable = CompositeDisposable()

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.run {
            putParcelableArrayList(LIST_ITEM_KEY, ArrayList(mAdapter.getItems()))
            putString(TV_ID_TEXT, edit_id.text.toString())
            putString(TV_COUNT_TEXT, edit_count.text.toString())
            val array = IntArray(2)
            array[0] = main_scroll_view.scrollX
            array[1] = main_scroll_view.scrollY
            putIntArray(POSITION_SCROLL_VIEW, array)
        }
        super.onSaveInstanceState(outState)

    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.run{
            edit_id.setText(this.getString(TV_ID_TEXT, ""))
            edit_count.setText(this.getString(TV_COUNT_TEXT, ""))
        }
        val list = savedInstanceState?.getParcelableArrayList<VkWallEntity>(LIST_ITEM_KEY)?.toList()
        if(list == null) {
            mAdapter.setData(listOf())
        } else {
            mAdapter.setData(list)
            edit_id.clearFocus()
            edit_count.clearFocus()
            savedInstanceState?.run {
                val array = this.getIntArray(POSITION_SCROLL_VIEW)
                main_scroll_view.scrollX = array[0]
                main_scroll_view.scrollY = array[1]
            }
        }
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initLayout()
        createHandlers()

    }

    private fun createHandlers() {
        button.setOnClickListener {
            mModel.loadWallMore(edit_id.text.toString(), edit_count.text.toString(), mAdapter.itemCount)

        }

        btn_show.setOnClickListener {
            showLoading()
            mModel.loadWall(edit_id.text.toString(), edit_count.text.toString())
        }

        disposable.addAll(
            mModel.getWallHandler()
                .subscribeOn(Schedulers.io())
                .map { json ->
                    mModel.parseResponse(json)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    if (response.error.isEmpty()) {
                        mAdapter.setData(response.list)
                        hiddenLoader()
                    }
                    else {
                        showError()
                        tv_error_msg.text = response.error
                    }
                }, {

                }),
            mModel.getWallMoreHandler()
                .subscribeOn(Schedulers.io())
                .map { json ->
                    mModel.parseResponse(json)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { response ->
                    if (response.error.isEmpty()) {
                        mAdapter.addData(response.list)
                        rv_wall.visibility = View.VISIBLE
                    }
                    else{
                        showError()
                        tv_error_msg.text = response.error
                    }
                }
        )
    }

    private fun showError(){
        rv_wall.visibility = View.GONE
        tv_error_msg.visibility = View.VISIBLE
        pb_loading.visibility = View.GONE
    }

    private fun hiddenLoader(){
        rv_wall.visibility = View.VISIBLE
        tv_error_msg.visibility = View.GONE
        pb_loading.visibility = View.GONE
    }

    private fun showLoading(){
        rv_wall.visibility = View.GONE
        tv_error_msg.visibility = View.GONE
        pb_loading.visibility = View.VISIBLE
    }

    private fun initLayout(){
        rv_wall.layoutManager = LinearLayoutManager(baseContext)
        rv_wall.isNestedScrollingEnabled = false
        mModel = WallModel()
        mAdapter = VkWallAdapter(mModel)
        rv_wall.adapter = mAdapter
    }

    interface DestroyListener{
        fun onDestroy()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }
}
