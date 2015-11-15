package ru.futurobot.longimageswithglide

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import ru.futurobot.longimageswithglide.liglide.LongImageView
import ru.futurobot.longimageswithglide.misc.Size

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
                .map { i -> AdapterData(i) }

        /**
         * Image measure callback can tell us size of concrete image
         * So we can store it for later use
         */
        private val imageMeasureCallback = object : LongImageView.MeasureCallback {
            override fun onMeasure(url: String, size: Size) {
                imgUrls.find { i -> i.url == url }?.size = size //TODO: i think it will be too slow
            }
        }

        override fun getItemCount(): Int {
            return imgUrls.size
        }

        override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
            val item = imgUrls[position]
            holder!!.textView.text = if (item.size.hasBothSize()) "${item.url} (${item.size.width} x ${item.size.height})" else "${item.url}"
            holder!!.imageView.measureCallback = imageMeasureCallback
            //If we have size of image
            //That performs instant resize of image view
            holder!!.imageView.displayImage(item.url, item.size)
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder? {
            return ViewHolder(LayoutInflater.from(parent!!.context).inflate(R.layout.list_item_layout, parent!!, false))
        }
    }

    /**
     * Adapter data class
     */
    private data class AdapterData(val url: String, var size: Size = Size(0, 0))

    /**
     * View holder for items
     */
    private class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: LongImageView = view.findViewById(R.id.imageView) as LongImageView
        val textView: TextView = view.findViewById(R.id.textView) as TextView
    }
}
