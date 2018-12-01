package com.developer.java.yandex.vkwall.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.developer.java.yandex.vkwall.R
import com.developer.java.yandex.vkwall.entity.VkWallEntity
import java.text.SimpleDateFormat

class VkWallAdapter : RecyclerView.Adapter<VkWallAdapter.ViewHolder>() {

    private var mData = listOf<VkWallEntity>()

    public fun setData(list : List<VkWallEntity>){
        mData = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VkWallAdapter.ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.rv_item_text, parent, false))
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    override fun onBindViewHolder(holder: VkWallAdapter.ViewHolder, position: Int) {
        holder.init(mData[position])
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name = view.findViewById<TextView>(R.id.item_title)
        val date = view.findViewById<TextView>(R.id.item_date)
        val content = view.findViewById<TextView>(R.id.item_content)
        val logo = view.findViewById<ImageView>(R.id.item_logo)

        fun init(item : VkWallEntity){
            name.text = item.name
            date.text = SimpleDateFormat("dd.MM.yy hh:mm").format(item.date)
            content.text = item.content
        }
    }
}