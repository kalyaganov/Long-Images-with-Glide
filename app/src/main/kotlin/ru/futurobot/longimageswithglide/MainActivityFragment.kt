package ru.futurobot.longimageswithglide

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.TextView
import ru.futurobot.longimageswithglide.liglide.LongImageView
import ru.futurobot.longimageswithglide.misc.Size

/**
 * A placeholder fragment containing a simple view.
 */
class MainActivityFragment : Fragment() {

    private val KEY_CURRENT_MODE = "KEY_CURRENT_MODE"

    private var recyclerView: RecyclerView? = null
    private var adapter: ImageAdapter? = null
    private var currentMode = R.id.glide_region_loader
    private var sharedPrefs: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        sharedPrefs = context?.getSharedPreferences(context?.packageName, Context.MODE_PRIVATE)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentMode = sharedPrefs!!.getInt(KEY_CURRENT_MODE, R.id.thread_region_loader)
        adapter = ImageAdapter(currentMode)
        recyclerView = view!!.findViewById(R.id.recyclerView) as RecyclerView
        recyclerView!!.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView!!.adapter = adapter
        recyclerView!!.setHasFixedSize(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.fragment_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.glide_region_loader || item?.itemId == R.id.thread_region_loader) {
            item!!.setChecked(true)
            adapter?.decoderMode = item!!.itemId
            adapter?.notifyDataSetChanged()
            return true
        }
        return false
    }

    /**
     * Recycler view image adapter
     */
    private class ImageAdapter(var decoderMode: Int) : RecyclerView.Adapter<ViewHolder>() {
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
            holder!!.imageView.regionLoader = if(decoderMode == R.id.glide_region_loader) LongImageView.REGION_LOADER_GLIDE else LongImageView.REGION_LOADER_THREAD
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
