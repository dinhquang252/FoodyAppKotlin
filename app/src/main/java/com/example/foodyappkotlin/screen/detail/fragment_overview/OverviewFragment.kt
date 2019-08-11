package com.example.foodyappkotlin.screen.detail.fragment_overview

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.DialogInterface
import android.location.Location
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.foodyappkotlin.AppSharedPreference
import com.example.foodyappkotlin.R
import com.example.foodyappkotlin.common.BaseFragment
import com.example.foodyappkotlin.data.models.BinhLuan
import com.example.foodyappkotlin.data.models.QuanAn
import com.example.foodyappkotlin.di.module.GlideApp
import com.example.foodyappkotlin.screen.adapter.CommentAdapter
import com.example.foodyappkotlin.screen.adapter.MonAnAdapter
import com.example.foodyappkotlin.screen.adapter.NuocUongAdapter
import com.example.foodyappkotlin.screen.detail.DetailEatingActivity
import com.example.foodyappkotlin.screen.detail.DetailViewModel
import com.example.foodyappkotlin.screen.detail.fragment_comments.FragmentComments
import com.example.foodyappkotlin.screen.detail.fragment_detail_comment.FragmentDetailComment
import com.example.foodyappkotlin.screen.detail.fragment_post.PostCommentFragment
import com.example.foodyappkotlin.screen.menu.MenuActivity
import com.example.foodyappkotlin.util.DateUtils
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_detail_eating.*
import kotlinx.android.synthetic.main.layout_feature.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.math.round

class OverviewFragment : BaseFragment(), OverviewInterface.View, MonAnAdapter.MonAnOnClickListener,
    NuocUongAdapter.NuocUongOnClickListener, CommentAdapter.CommentOnCLickListerner {

    lateinit var inputParser: SimpleDateFormat
    lateinit var detailViewModel: DetailViewModel
    lateinit var commentAdapter: CommentAdapter
    lateinit var dataRef: Query
    lateinit var childEventListener: ChildEventListener
    private lateinit var mQuanAn: QuanAn

    private var diemQuanAn = 0.0
    val storage = FirebaseStorage.getInstance().reference

    @Inject
    lateinit var mActivity: DetailEatingActivity

    @Inject
    lateinit var appSharedPreference: AppSharedPreference

    companion object {
        private const val INPUT_FORMAT = "HH:mm"
        fun newInstance(): Fragment {
            val overviewFragment = OverviewFragment()
            return overviewFragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        AndroidSupportInjection.inject(this)
        return inflater.inflate(R.layout.fragment_detail_eating, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
    }

    fun initData() {
        inputParser = SimpleDateFormat(INPUT_FORMAT, Locale.ENGLISH)
        detailViewModel = activity?.run {
            ViewModelProviders.of(this).get(DetailViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        detailViewModel.quanan.observe(this, Observer<QuanAn> { item ->
            findQuanAnData(item!!)
            findCommentData(ArrayList())
            /* if (item.binhluans.isNotEmpty()) {
                 recycler_user_comment.visibility = View.VISIBLE
                 text_view_all_comment.text = "Xem thêm"
                 val binhluans = ArrayList<BinhLuan>(item.binhluans.values)
                 findCommentData(binhluans)
             } else {
                 recycler_user_comment.visibility = View.GONE
                 text_view_all_comment.text = "Hãy là người đầu tiên đánh giá quán ăn"
             }*/
//            findThucDonData(item.thucdons)
        })

        ln_post_comment.setOnClickListener {
            mActivity.pushFragment(R.id.layout_food_detail, PostCommentFragment.newInstance())
        }

        text_view_all_comment.setOnClickListener {
            mActivity.pushFragment(R.id.layout_food_detail, FragmentComments.newInstance())
        }
/*        swiperefresh.setOnRefreshListener {
            commentAdapter.clearAllData()
            reloadBinhLuanQuanAn()
        }*/
    }

    fun reloadBinhLuanQuanAn() {
        getAllCommentFollowQuanAn()
    }

    fun findQuanAnData(quanAn: QuanAn) {
        mQuanAn = quanAn

        if (appSharedPreference.getLocation() != null) {
            text_distance.text = "Cách bạn: ${distance(
                appSharedPreference.getLocation().latitude,
                appSharedPreference.getLocation().longitude,
                quanAn.latitude,
                quanAn.longitude
            )} km"
        }
        quanAn.binhluans.mapNotNull {
            diemQuanAn += it.value.chamdiem / 2
        }
        if (diemQuanAn > 0) {
            diemQuanAn /= quanAn.binhluans.size
        }
        text_point.text = "${(round(diemQuanAn))}"

        text_order.text = "giao hàng: ${quanAn.giaohang}"
        if(quanAn.giaohang){
            button_order.visibility = View.VISIBLE
            button_order.setOnClickListener {
                startActivity(MenuActivity.newInstance(context!!))
            }
        }else{
            button_order.visibility = View.GONE
        }

        if ((quanAn.hinhanhs.isNotEmpty())) {
            var storageRef: StorageReference
            val hinhAnhQuanAn = ArrayList<String>()
            quanAn.hinhanhs.forEach {
                hinhAnhQuanAn.add(it.value)
            }
            storageRef = storage.child("monan").child(hinhAnhQuanAn[0])
            GlideApp.with(activityContext)
                .load(storageRef)
                .error(R.drawable.placeholder)
                .thumbnail(0.1f)
                .placeholder(R.drawable.placeholder)
                .into(image_eating)
        }

        var sum = 0
        text_sum_comment.text = quanAn.binhluans.size.toString()
        quanAn.binhluans.forEach {
            if (it.value.hinhanh.size > 0) {
                sum += it.value.hinhanh.size
            }
        }.let {
            text_count_image.text = sum.toString()
        }
        text_name_eating.text = quanAn.tenquanan
        text_location.text = quanAn.diachi

//        text_distance
        text_status.text =
            "Mở cửa:${DateUtils.convertMinuteToHours(quanAn.giomocua)} - Đóng cửa:${DateUtils.convertMinuteToHours(
                quanAn.giodongcua
            )}"
//        recycler_menu.isNestedScrollingEnabled = false
    }

    fun getAllCommentFollowQuanAn() {
        dataRef =
            FirebaseDatabase.getInstance().reference.child("quanans").child("KV1").child(mQuanAn.id)
                .child("binhluans")
        var binhluans = ArrayList<BinhLuan>()

        childEventListener = object : ChildEventListener {
            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                val comment = p0.getValue(BinhLuan::class.java)
                if (comment != null) {
                    binhluans.add(comment)
                    getAllCommentSuccess(comment)
                } else {
                }
            }

            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {
            }

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {
            }

            override fun onChildRemoved(p0: DataSnapshot) {
                val binhLuan = p0.getValue(BinhLuan::class.java)
                if (binhLuan != null) {
                    commentAdapter.removeComment(binhLuan)
                }
            }
        }
        dataRef.addChildEventListener(childEventListener)
    }


/*
    fun findThucDonData(thucDons: MutableList<ThucDonResponse>) {
        recycler_menu.visibility = View.VISIBLE
        text_menu_viewmore.text = "Xem thêm"
        recycler_menu.visibility = View.GONE
        text_menu_viewmore.text = "Quán ăn chưa có thực đơn"

        if (thucDons.size > 0) {
            monAnAdapter = MonAnAdapter(activity!!, thucDons, MonAnAdapter.TYPE_VIEW, this)
            recycler_menu.adapter = monAnAdapter
        }
    }
*/

    fun findCommentData(binhluans: ArrayList<BinhLuan>) {
        commentAdapter =
            CommentAdapter(activity!!, binhluans,
                appSharedPreference.getToken()!!, appSharedPreference.getUser().liked, this)
        recycler_user_comment.layoutManager = LinearLayoutManager(activityContext)
        recycler_user_comment.adapter = commentAdapter
        recycler_user_comment.isNestedScrollingEnabled = false
        getAllCommentFollowQuanAn()
    }


    fun distance(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
        val loc1 = Location("")
        loc1.latitude = lat1
        loc1.longitude = long1

        val loc2 = Location("")
        loc2.latitude = lat2
        loc2.longitude = long2
        val distance = Math.round(loc1.distanceTo(loc2) / 1000 * 100) / 100.0
        return distance
    }

    override fun monAnCalculatorMoney(money: Long) {
    }

    override fun nuocUongCalculatorMoney(money: Long) {
    }

    override fun getAllCommentSuccess(comment: BinhLuan) {
        commentAdapter.onDataChanged(comment)
    }

    override fun getAllCommentFailure(message: String) {
        Log.d("kiemtra", message)
    }

    override fun onClickItemCommentListerner(binhLuan: BinhLuan) {
        if (mQuanAn.id != "") {
            mActivity.pushFragment(
                R.id.layout_food_detail,
                FragmentDetailComment.newInstance(mQuanAn, binhLuan)
            )
        }
    }

    fun cleanupListener() {
        childEventListener?.let {
            dataRef.removeEventListener(it)
        }
    }

    override fun onClickEditComment(binhLuan: BinhLuan) {
//        mActivity.pushFragment(R.id.frame_layout,PostCommentFragment.newInstance())
    }

    override fun onClickDeleteComment(binhLuan: BinhLuan) {
        showAlertListerner("Xác nhận xóa Comment!", "Bạn có muốn xóa comment này?",
            DialogInterface.OnClickListener { _, _ ->
                FirebaseDatabase.getInstance().reference.child("quanans")
                    .child("KV${mQuanAn.id_khuvuc}").child(mQuanAn.id).child("binhluans").child(binhLuan.id).removeValue()
            })
    }

    override fun onStop() {
        super.onStop()
        cleanupListener()
    }
}
