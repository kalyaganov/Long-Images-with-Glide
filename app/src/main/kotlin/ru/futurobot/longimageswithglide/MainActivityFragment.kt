package ru.futurobot.longimageswithglide

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide

/**
 * A placeholder fragment containing a simple view.
 */
class MainActivityFragment : Fragment() {

    var recyclerView: RecyclerView? = null
    var adapter: ImageAdapter? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ImageAdapter()
        recyclerView = view!!.findViewById(R.id.recyclerView) as RecyclerView
        recyclerView!!.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView!!.adapter = adapter
        recyclerView!!.setHasFixedSize(true)
    }

    /**
     * Recycler view image adapter
     */
    private class ImageAdapter() : RecyclerView.Adapter<ViewHolder>() {
        private val imgUrls = arrayOf(
                "http://cs5.pikabu.ru/post_img/2015/11/14/5/1447482219_790545089.jpg",
                "http://cs4.pikabu.ru/post_img/2015/11/14/6/1447491411_533566373.jpeg",
                "http://cs5.pikabu.ru/post_img/2015/11/14/6/1447488312_1412165079.jpg",
                "http://cs5.pikabu.ru/post_img/2015/11/14/6/1447490442_835353174.jpg",
                "http://cs5.pikabu.ru/post_img/2015/11/13/11/1447439487_989003459.jpeg",
                "http://cs5.pikabu.ru/post_img/2015/11/13/12/1447445495_493031746.jpg")

        override fun getItemCount(): Int {
            return imgUrls.size
        }

        override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
            holder!!.textView.text = imgUrls[position]
            var imageView = holder!!.imageView
            var context = imageView.context
            Glide.with(context)
                    .load(imgUrls[position])
                    .dontAnimate()
                    .placeholder(R.drawable.gray)
                    .centerCrop()
                    .into(imageView)
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder? {
            return ViewHolder(LayoutInflater.from(parent!!.context).inflate(R.layout.list_item_layout, parent!!, false))
        }
    }

    /**
     * View holder for items
     */
    private class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView) as ImageView
        val textView: TextView = view.findViewById(R.id.textView) as TextView
    }
}
