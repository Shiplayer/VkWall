package com.developer.java.yandex.vkwall.adapter

import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.developer.java.yandex.vkwall.R
import com.developer.java.yandex.vkwall.entity.User
import com.developer.java.yandex.vkwall.entity.VkWallEntity
import com.developer.java.yandex.vkwall.interactors.UserInteractor
import com.developer.java.yandex.vkwall.model.WallModel
import io.reactivex.android.schedulers.AndroidSchedulers
import java.net.URL
import java.text.SimpleDateFormat
import java.util.concurrent.Executors

class VkWallAdapter(var model: WallModel): RecyclerView.Adapter<VkWallAdapter.ViewHolder>() {

    private var mData = mutableListOf<VkWallEntity>()

    public fun setData(list : List<VkWallEntity>){
        mData.clear()
        mData.addAll(list)
        notifyDataSetChanged()
    }

    fun addData(list : List<VkWallEntity>){
        val position = mData.size
        mData.addAll(position, list)
        notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VkWallAdapter.ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.rv_item_text, parent, false), model)
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    override fun onBindViewHolder(holder: VkWallAdapter.ViewHolder, position: Int) {
        holder.init(mData[position])

        /*
        val user = model.getUser(item.id, object : UserInteractor {
            override fun onSuccessful(user: User) {
                notifyItemChanged(position)
            }

            override fun onError(throwable: Throwable) {
                Log.i("VkWallAdapterError", throwable.message)
                throwable.printStackTrace()
            }
        })
        if(user != null){

        }
         */
    }

    public fun load(){

    }

    class ViewHolder(view: View, val model:WallModel) : RecyclerView.ViewHolder(view) {
        val name = view.findViewById<TextView>(R.id.item_title)
        val date = view.findViewById<TextView>(R.id.item_date)
        val content = view.findViewById<TextView>(R.id.item_content)
        val logo = view.findViewById<ImageView>(R.id.item_logo)

        fun init(item : VkWallEntity){
            val user = model.getUser(item.id, object : UserInteractor{
                override fun onSuccessful(user: User) {
                    showUser(user)
                }

                override fun onError(throwable: Throwable) {
                    Log.i("VkWallAdapterError", throwable.message)
                    throwable.printStackTrace()
                }
            })
            if(user != null){
                showUser(user)
            }
            content.text = item.content
            date.text = SimpleDateFormat("dd.MM.yy HH:mm").format(item.date)
        }

        fun showUser(user: User){
            name.text = user.firstName + " " + user.lastName
            var url = URL(user.urlPhoto)
            Executors.newSingleThreadExecutor().submit {
                var logoDrawable = Drawable.createFromStream(url.openStream(), "logo${user.id}")
                AndroidSchedulers.mainThread().scheduleDirect {
                    logo.setImageDrawable(logoDrawable)
                }
            }
        }
    }
}