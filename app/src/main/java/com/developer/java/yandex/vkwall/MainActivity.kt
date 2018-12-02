package com.developer.java.yandex.vkwall

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.developer.java.yandex.vkwall.adapter.VkWallAdapter
import com.developer.java.yandex.vkwall.model.WallModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        public val token = "a10b7fd7a10b7fd7a10b7fd715a144c62baa10ba10b7fd7fafe18ad246c4fcef5af6a9f"
    }

    private lateinit var mAdapter: VkWallAdapter
    private lateinit var mModel: WallModel
    private val disposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rv_wall.layoutManager = LinearLayoutManager(baseContext)
        mModel = WallModel()
        mAdapter = VkWallAdapter(mModel)
        rv_wall.adapter = mAdapter

        val calendar = Calendar.getInstance()
        button.setOnClickListener {
            mModel.loadWallMore(edit_id.text.toString(), edit_count.text.toString()).subscribeOn(AndroidSchedulers.mainThread())
                .subscribe({list ->
                    mAdapter.addData(list)
                }, {

                })
        }

        btn_show.setOnClickListener {
            mModel.loadWall(edit_id.text.toString(), edit_count.text.toString())
                .subscribeOn(AndroidSchedulers.mainThread())
                .doAfterNext{
                    rv_wall.visibility = View.VISIBLE
                    pb_loading.visibility = View.GONE
                }
                .subscribe {list ->
                    mAdapter.setData(list)
                }
            /*val id = edit_id.text.toString().toIntOrNull()
            val count: Int? = if (edit_count.text.isEmpty()) null else edit_count.text.toString().toInt()

            val obs = if (id == null) {
                VkApiService.api.getWall(domain = edit_id.text.toString(), count = count, accessToken = token)
            } else {
                VkApiService.api.getWall(id = id, count = count, accessToken = token)
            }*/
            //disposable.add()
        }

    }

    interface DestroyListener{
        fun onDestroy()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }
}
