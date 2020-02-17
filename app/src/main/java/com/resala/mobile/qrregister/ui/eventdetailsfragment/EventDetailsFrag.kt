/*
 * Created by  Mobile Dev Team  on 1/11/20 6:22 PM
 * Copyright (c) Resala Charity Organization. All rights reserved.
 */

package com.resala.mobile.qrregister.ui.eventdetailsfragment

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.zxing.Result
import com.jakewharton.rxbinding2.widget.RxTextView
import com.resala.mobile.qrregister.R
import com.resala.mobile.qrregister.databinding.FragEventDetailsBinding
import com.resala.mobile.qrregister.shared.dialogs.DialogScanSuccessFragment
import com.resala.mobile.qrregister.shared.ui.frag.BaseFrag
import com.resala.mobile.qrregister.shared.util.FlashbarUtil
import com.resala.mobile.qrregister.shared.util.ext.showError
import kotlinx.android.synthetic.main.sheet_new_vlounteer.view.*
import me.dm7.barcodescanner.zxing.ZXingScannerView
import org.koin.android.viewmodel.ext.android.viewModel


class EventDetailsFrag : BaseFrag<EventDetailsVm>(), ZXingScannerView.ResultHandler {


    override val vm: EventDetailsVm by viewModel()
    override var layoutId: Int = R.layout.frag_event_details

    private var mScannerView: ZXingScannerView? = null
    private lateinit var viewDataBinding: FragEventDetailsBinding
    private var mBehavior: BottomSheetBehavior<*>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewDataBinding = FragEventDetailsBinding.inflate(inflater, container, false).apply {
            viewmodel = vm
        }

        // Set the lifecycle owner to the lifecycle of the view
        viewDataBinding.lifecycleOwner = this.viewLifecycleOwner
        viewDataBinding.executePendingBindings()
        return viewDataBinding.root
    }

    override fun doOnViewCreated() {
        super.doOnViewCreated()
        initView()
        checkValidations()
    }

    private fun checkValidations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(
                    activity()!!,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startCamera()
            } else {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISSION_CODE)
            }
        } else {
            startCamera()
        }
    }

    private fun initView() {
        setToolBar()
        setupRegisterField()
        mScannerView = ZXingScannerView(activity)
        viewDataBinding.contentFrame!!.addView(mScannerView)
        mBehavior = BottomSheetBehavior.from<View>(viewDataBinding.root.new_volunteer_sheet)
        showBottomSheetDialog()

        vm.responseRegisterBody.observe(
            this,
            Observer {
                when {
                    it.isLoading -> {
                        vm.showHideDots(true)
                    }
                    it.result != null -> {

                        vm.toggleRevealView(false)
                        showSuccessDialog()

                    }
                    it.errorMessage != null -> {
                        vm.showHideDots(false)
                        it.errorMessage.showError(context()!!)
                    }
                    it.isOffline -> {

                    }

                }
            }
        )
    }

    private fun setToolBar() {
        viewDataBinding.toolbar.title = EventDetailsFragArgs.fromBundle(arguments!!).title
        viewDataBinding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
        viewDataBinding.toolbar.setNavigationOnClickListener { activity()!!.onBackPressed() }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun handleResult(rawResult: Result?) {

        Log.e("QrResult", rawResult?.text.toString())
        registerVolunteerApi(rawResult!!)
        // Note:
        // * Wait 2 seconds to resume the preview.
        // * On older devices continuously stopping and resuming camera preview can result in freezing the app.
        // * I don't know why this is the case but I don't have the time to figure out.
        Handler().postDelayed({
            mScannerView!!.resumeCameraPreview(this)
        }, 200)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun registerVolunteerApi(rawResult: Result) {
        FlashbarUtil.show(
            "Contents = " + rawResult.text + ", Format = " + rawResult.barcodeFormat.toString(),
            activity = activity()!!
        )

        vm.registerVolunteerByCodeOrNumber("1", "1", "1", "")
    }

    private fun showSuccessDialog() {
        val fragmentManager = childFragmentManager
        val newFragment = DialogScanSuccessFragment()
        val args = Bundle()
        args.putString("NAME", "Muhammad Sayed")
        args.putString("EVENT", "Qwaphil Ghargi")
        newFragment.arguments = args
        val transaction = fragmentManager.beginTransaction()
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        transaction.add(R.id.dialogFrame, newFragment).addToBackStack(null)
            .commitAllowingStateLoss()
    }

    private fun startCamera() {
        mScannerView!!.setResultHandler(this)
        mScannerView?.setAutoFocus(true)
        mScannerView!!.startCamera()
    }

    override fun onResume() {
        super.onResume()
        startCamera()
    }


    override fun onPause() {
        super.onPause()
        stopCamera()
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>, @NonNull grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                startCamera()
            } else {
                stopCamera()
                FlashbarUtil.show(
                    getString(R.string.please_grant_camera_permission),
                    activity = activity()!!
                )

            }
        }
    }

    private fun stopCamera() {
        mScannerView!!.setResultHandler(this)
        mScannerView!!.stopCamera()
    }

    private fun showBottomSheetDialog() {


        if (mBehavior!!.state == BottomSheetBehavior.STATE_EXPANDED) {
            mBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        vm.toggleSheet.observe(this, Observer {
            mBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
        })


        mBehavior!!.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        vm.showHideText(true)
                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        vm.showHideText(false)
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        vm.showHideText(true)
                    }
                    BottomSheetBehavior.STATE_DRAGGING -> {
                    }
                    BottomSheetBehavior.STATE_SETTLING -> {
                    }
                    BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                    }
                }
            }
        })
    }

    @SuppressLint("CheckResult")
    private fun setupRegisterField() {
        RxTextView.textChanges(viewDataBinding.etId).subscribe { text ->
            registerCodeBtnVisibilty(text)
        }

        RxTextView.textChanges(viewDataBinding.etNumber).subscribe { text ->

            registerNumberBtnVisibilty(text)

        }
        viewDataBinding.btnSendCodeOrID.setOnClickListener {
            vm.registerVolunteerByCodeOrNumber("1", "1", "1", "")
        }

        viewDataBinding.btnSendNumber.setOnClickListener {
            vm.registerVolunteerByCodeOrNumber("1", "", "1", "010")
        }


        viewDataBinding.newVolunteerSheet.btnRegisterData.setOnClickListener {
            //introduce data validation before registering
            vm.registerVolunteerByData(
                viewDataBinding.newVolunteerSheet.etEmail.text.toString(),
                viewDataBinding.newVolunteerSheet.etBranchId.text.toString(),
                viewDataBinding.newVolunteerSheet.etEventId.text.toString(),
                viewDataBinding.newVolunteerSheet.spinnerGender.selectedItem.toString(),
                viewDataBinding.newVolunteerSheet.etName.text.toString(),
                viewDataBinding.newVolunteerSheet.etPhoneNumber.text.toString(),
                viewDataBinding.newVolunteerSheet.etRegionId.text.toString()

            )
        }


    }


    private fun registerNumberBtnVisibilty(text: CharSequence?) {
        if (text.isNullOrEmpty())
            viewDataBinding.btnSendNumber.visibility = View.GONE
        else
            viewDataBinding.btnSendNumber.visibility = View.VISIBLE
    }

    private fun registerCodeBtnVisibilty(text: CharSequence?) {
        if (text.isNullOrEmpty())
            viewDataBinding.btnSendCodeOrID.visibility = View.GONE
        else
            viewDataBinding.btnSendCodeOrID.visibility = View.VISIBLE
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun enterReveal(myView: View, rawResult: Result) { // previously invisible view
        // get the center for the clipping circle
        val cx = myView.measuredWidth / 2
        val cy = myView.measuredHeight / 2
        // get the final radius for the clipping circle
        val finalRadius = Math.max(myView.width, myView.height) / 2
        // create the animator for this view (the start radius is zero)
        val anim =
            ViewAnimationUtils.createCircularReveal(myView, cx, cy, 0f, finalRadius.toFloat())
        // make the view visible and start the animation

        vm.toggleRevealView(true)
        anim.start()
        // make the view invisible when the animation is done
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)

                vm.toggleRevealView(false)
                showSuccessDialog()
                FlashbarUtil.show(
                    "Contents = " + rawResult.text + ", Format = " + rawResult.barcodeFormat.toString(),
                    activity = activity()!!
                )
            }
        })

    }

    companion object {
        private var PERMISSION_CODE = 123
    }
}

