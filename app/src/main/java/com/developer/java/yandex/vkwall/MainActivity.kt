package com.developer.java.yandex.vkwall

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import com.developer.java.yandex.vkwall.adapter.VkWallAdapter
import com.developer.java.yandex.vkwall.retrofit.VkApiService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var mAdapter: VkWallAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rv_wall.layoutManager = LinearLayoutManager(baseContext)
        mAdapter = VkWallAdapter()
        rv_wall.adapter = mAdapter

        btn_show.setOnClickListener {
            val id = edit_id.text.toString().toInt()
            val count:Int? = if(edit_count.text.isEmpty()) null else edit_count.text.toString().toInt()
            val token = "a10b7fd7a10b7fd7a10b7fd715a144c62baa10ba10b7fd7fafe18ad246c4fcef5af6a9f"

            val obs = if(count != null)
                VkApiService.api.getWall(id, count, token)
            else
                VkApiService.api.getAllWall(id, token)
            obs.subscribeOn(Schedulers.io()).doFinally {
                runOnUiThread {
                    frame_loader.visibility = View.GONE
                    rv_wall.visibility = View.VISIBLE
                }
            }.subscribe({
                runOnUiThread {
                    Log.i("MainActivity", it.toString())
                }
            }, {
                Log.i("MainActivity", it.message)
                it.printStackTrace()
            })
        }

    }
}
