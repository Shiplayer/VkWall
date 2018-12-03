package com.developer.java.yandex.vkwall

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.widget.NestedScrollView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.developer.java.yandex.vkwall.adapter.VkWallAdapter
import com.developer.java.yandex.vkwall.entity.VkWallEntity
import com.developer.java.yandex.vkwall.model.WallModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity() {
    /**
     * Используется для уведомления о том, что приложение уничтожается и нужно освободить ресурсы
     */
    interface DestroyListener {
        fun onDestroy()
    }

    companion object {
        /**
         * Токен для получения данных от vk api
         */
        const val TOKEN = "a10b7fd7a10b7fd7a10b7fd715a144c62baa10ba10b7fd7fafe18ad246c4fcef5af6a9f"

        /**
         * Ключи, по которым сохраняем состояния и восстанавливаем
         */
        private const val LIST_ITEM_KEY: String = "listItemKey"
        private const val TV_ID_TEXT: String = "textViewIdText"
        private const val TV_COUNT_TEXT: String = "textViewCountText"
        private const val POSITION_SCROLL_VIEW: String = "positionScrollView"
        private const val FIND_WALL_ID: String = "findWallId"
        private const val FIND_WALL_COUNT: String = "findWallCount"
        private const val TOTAL_WALL_POST_COUNT: String = "totalWallPostCount"
        private const val SHOW_EMPTY_WALL: String = "showEmptyWall"
    }

    /**
     * Адаптер, который содержит элементы записей на странице
     */
    private lateinit var mAdapter: VkWallAdapter

    /**
     * Модель, с помощью которое осуществлено взаимодействие с сервером для отправки запросов
     */
    private lateinit var mModel: WallModel

    private val mDisposable = CompositeDisposable()

    /**
     * Общее количество постов
     */
    private var mTotalWallPost: Int? = null

    /**
     * Айди или короткое имя пользователя или сообщества
     */
    private var mWallId: String? = null

    /**
     * Количество, которое необходимо получить
     */
    private var mWallCount: String? = null

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.run {
            putParcelableArrayList(LIST_ITEM_KEY, ArrayList(mAdapter.getItems()))
            putString(TV_ID_TEXT, edit_id.text.toString())
            putString(TV_COUNT_TEXT, edit_count.text.toString())
            val array = IntArray(2)
            array[0] = main_scroll_view.scrollX
            array[1] = main_scroll_view.scrollY
            putIntArray(POSITION_SCROLL_VIEW, array)
            if (mWallId != null) {
                putString(FIND_WALL_ID, mWallId)
            }
            if (mWallCount != null) {
                putString(FIND_WALL_COUNT, mWallCount)
            }
            if (mTotalWallPost != null) {
                putInt(TOTAL_WALL_POST_COUNT, mTotalWallPost!!)
            }

            putBoolean(SHOW_EMPTY_WALL, cb_show_empty.isChecked)
        }
        super.onSaveInstanceState(outState)

    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.run {
            edit_id.setText(this.getString(TV_ID_TEXT, ""))
            edit_count.setText(this.getString(TV_COUNT_TEXT, ""))
            if (this.containsKey(FIND_WALL_ID)) {
                mWallId = this.getString(FIND_WALL_ID)
            }
            if (this.containsKey(FIND_WALL_COUNT)) {
                mWallCount = this.getString(FIND_WALL_COUNT)
            }
            if (this.containsKey(TOTAL_WALL_POST_COUNT)) {
                mTotalWallPost = getInt(TOTAL_WALL_POST_COUNT)
            }
            cb_show_empty.isChecked = getBoolean(SHOW_EMPTY_WALL, false)
        }
        val list = savedInstanceState?.getParcelableArrayList<VkWallEntity>(LIST_ITEM_KEY)?.toList()
        if (list == null) {
            mAdapter.setData(listOf(), cb_show_empty.isChecked)
        } else {
            mAdapter.setData(list, cb_show_empty.isChecked)
            edit_id.clearFocus()
            edit_count.clearFocus()
            savedInstanceState.run {
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

    /**
     * Инициализация обработчиков событий
     */
    private fun createHandlers() {

        cb_show_empty.setOnCheckedChangeListener { compoundButton, checked ->
            mAdapter.setFilter(checked)
        }

        btn_clear.setOnClickListener {
            cb_show_empty.isChecked = false
            mAdapter.setData(listOf(), false)
            edit_id.setText("")
            edit_count.setText("")
            mWallId = null
            mWallCount = null
            mTotalWallPost = null
            tv_total_count.text = ""
        }

        main_scroll_view.setOnScrollChangeListener { v: NestedScrollView?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int ->
            if (v!!.getChildAt(v.childCount - 1) != null && rv_wall.layoutManager.childCount > 0) {
                if ((scrollY >= (v.getChildAt(v.childCount - 1).measuredHeight - v.measuredHeight)) && scrollY > oldScrollY) {
                    val visibleItemCount = rv_wall.layoutManager.childCount
                    val totalItemCount = rv_wall.layoutManager.itemCount
                    val pastVisibleCount = if (rv_wall.layoutManager is LinearLayoutManager) {
                        (rv_wall.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                    } else {
                        (rv_wall.layoutManager as GridLayoutManager).findFirstVisibleItemPosition()
                    }

                    if (mTotalWallPost != null && mTotalWallPost!! > 0)
                        if ((visibleItemCount + pastVisibleCount) >= totalItemCount && mWallId != null && mWallCount != null) {
                            mModel.loadWallMore(mWallId!!, mWallCount!!, mAdapter.getOriginItemCount())
                        }
                }
            }
        }

        /*button.setOnClickListener {
            mModel.loadWallMore(edit_id.text.toString(), edit_count.text.toString(), mAdapter.itemCount)

        }*/

        btn_show.setOnClickListener {
            mTotalWallPost = null
            mWallId = edit_id.text.toString()
            mWallCount = edit_count.text.toString()
            showLoading()
            mModel.loadWall(mWallId!!, mWallCount!!)
        }

        mDisposable.addAll(
            createWallHandler(),
            createWallMoreHandler()
        )
    }

    /**
     * Создание наблюдателя за получением ответа от сервера для получения еще большего количества постов
     */
    private fun createWallMoreHandler(): Disposable {
        return mModel.getWallMoreHandler()
            .subscribeOn(Schedulers.io())
            .map { json ->
                val response = mModel.parseResponse(json)
                response
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { response ->
                if (response.error.isEmpty()) {
                    mAdapter.addData(response.list, cb_show_empty.isChecked)
                    rv_wall.visibility = View.VISIBLE
                } else {
                    Snackbar.make(rv_wall, response.error, Snackbar.LENGTH_LONG).show()
                }
            }
    }

    /**
     * Создание наблюдателя за получением ответа от сервера для получения постов
     */
    @SuppressLint("SetTextI18n")
    private fun createWallHandler(): Disposable {
        return mModel.getWallHandler()
            .subscribeOn(Schedulers.io())
            .map { json ->
                val response = mModel.parseResponse(json)
                response
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                if (mTotalWallPost == null || mTotalWallPost == -1) {
                    mTotalWallPost = response.total
                    tv_total_count.text = resources.getString(R.string.total_count) + " " + response.total
                }
                if (response.error.isEmpty()) {
                    mAdapter.setData(response.list, cb_show_empty.isChecked)
                    hiddenLoader()
                } else {
                    showError()
                    tv_error_msg.text = response.error
                }
            }, {

            })
    }

    /**
     * Для получения названия разрешения экрана
     * @param context - необходим для получения разрешения экрана
     * @return строка, в которой содержится название разрешения экрана
     */
    private fun getSizeName(context: Context): String {
        var screenLayout = context.resources.configuration.screenLayout
        screenLayout = screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK

        return when (screenLayout) {
            Configuration.SCREENLAYOUT_SIZE_SMALL -> "small"
            Configuration.SCREENLAYOUT_SIZE_NORMAL -> "normal"
            Configuration.SCREENLAYOUT_SIZE_LARGE -> "large"
            4 // Configuration.SCREENLAYOUT_SIZE_XLARGE is API >= 9
            -> "xlarge"
            else -> "undefined"
        }
    }

    /**
     * Показать ошибку, если она произошла
     */
    private fun showError() {
        rv_wall.visibility = View.GONE
        tv_error_msg.visibility = View.VISIBLE
        pb_loading.visibility = View.GONE
    }

    /**
     * Скрыть прогресс бар и показать RecyclerView после загрузки постов
     */
    private fun hiddenLoader() {
        rv_wall.visibility = View.VISIBLE
        tv_error_msg.visibility = View.GONE
        pb_loading.visibility = View.GONE
    }

    /**
     * Показать прогресс бар, во время загрузки постов
     */
    private fun showLoading() {
        rv_wall.visibility = View.GONE
        tv_error_msg.visibility = View.GONE
        pb_loading.visibility = View.VISIBLE
    }

    private fun initLayout() {
        if (getSizeName(baseContext).contains("large")) {
            rv_wall.layoutManager = GridLayoutManager(baseContext, 2)
        } else {
            rv_wall.layoutManager = LinearLayoutManager(baseContext)
        }
        rv_wall.isNestedScrollingEnabled = false
        mModel = WallModel()
        mAdapter = VkWallAdapter(mModel)
        rv_wall.adapter = mAdapter
    }

    override fun onDestroy() {
        super.onDestroy()
        mDisposable.dispose()
    }
}
