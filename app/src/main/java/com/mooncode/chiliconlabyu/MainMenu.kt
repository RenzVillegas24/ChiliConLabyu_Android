package com.mooncode.chiliconlabyu

import android.Manifest
import android.R.attr.animationDuration
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Insets
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.doOnLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial


class MainMenu : Fragment() {

    private var isSticky = false
    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_main_menu, container, false)
        val btnSettings_v = view.findViewById<Button>(R.id.btnSettings)
        val headerCard_v = view.findViewById<MaterialCardView>(R.id.headerCard)
        val headerCardSticky_v = view.findViewById<MaterialCardView>(R.id.headerCardSticky)
        val headerCardStickyGrid_v = view.findViewById<GridLayout>(R.id.headerCardStickyGrid)
        val mainScroller_v = view.findViewById<ScrollView>(R.id.mainScroller)
        val mainContent_v = view.findViewById<LinearLayout>(R.id.mainContent)
        val layoutLightSlider_v = view.findViewById<LinearLayout>(R.id.layoutLightSlider)
        val textViewTitle_v = view.findViewById<TextView>(R.id.textViewTitle)

        val btnAutomatic_v = view.findViewById<SwitchMaterial>(R.id.btnAutomatic)
        val btnSprinkler_v = view.findViewById<SwitchMaterial>(R.id.btnSprinkler)
        val btnLight_v = view.findViewById<SwitchMaterial>(R.id.btnLight)
        val sliderLightIntensity_v = view.findViewById<Slider>(R.id.sliderLightIntensity)
        val textSliderLightIntensity_v = view.findViewById<TextView>(R.id.textSliderLightIntensity)


        btnLight_v.setOnCheckedChangeListener { compoundButton, b ->
            if (b){
                layoutLightSlider_v.visibility = LinearLayout.VISIBLE
            } else {
                layoutLightSlider_v.visibility = LinearLayout.GONE
            }
        }

        sliderLightIntensity_v.addOnChangeListener { slider, value, fromUser ->
            textSliderLightIntensity_v.text = String.format("Controlled Light Intensity: %.0f%%", value)
        }


        headerCardSticky_v.measure(0, 0)
        val stickyYPosition = getScreenInsets().top + headerCardSticky_v.measuredHeight
        mainContent_v.minimumHeight =  getScreenHeight() - headerCardSticky_v.measuredHeight

        fun stickyScroll(scrollY: Int) {

            val scrollHeight = getScreenHeight() + getScreenInsets().bottom - stickyYPosition
            val scrollWidth = getScreenWidth()
            val collapsePercentage = (scrollY * 1f)/(scrollHeight * 1f)


            if (collapsePercentage < 1){


                if(!isSticky){
                    val fade = ObjectAnimator.ofFloat(headerCardSticky_v, "alpha", 0f)
                    fade.duration = 100
                    fade.doOnEnd {
                        headerCardSticky_v.visibility = MaterialCardView.GONE
                    }
                    headerCard_v.visibility = MaterialCardView.VISIBLE
                    fade.start()

                    isSticky = true
                }


            } else {
                if(isSticky){
                    val fade = ObjectAnimator.ofFloat(headerCardSticky_v, "alpha",1f)
                    fade.duration = 100
                    fade.doOnEnd {
                        headerCard_v.visibility = MaterialCardView.GONE
                    }
                    headerCardSticky_v.visibility = MaterialCardView.VISIBLE
                    fade.start()

                    isSticky = false
                }


            }
        }


        mainScroller_v.setOnScrollChangeListener  { _, _, scrollY, _, oldScrollY ->
            stickyScroll(scrollY)
        }
        mainScroller_v.post {
            mainScroller_v.scrollTo(0, 0);
        }




        val params = FrameLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        params.setMargins(0,  getScreenHeight() - getScreenInsets().top - getScreenInsets().bottom - typeDP(40), 0,0)
        textViewTitle_v.layoutParams = params

        headerCardStickyGrid_v.setPadding(0,getScreenInsets().top ,0,0)


        (mainContent_v.layoutParams as? ViewGroup.MarginLayoutParams)
            ?.let {
                it.topMargin = getScreenHeight() + getScreenInsets().top
                mainContent_v.layoutParams = it
            }

        headerCard_v.layoutParams.width = getScreenWidth()
        headerCard_v.layoutParams.height = getScreenHeight() + getScreenInsets().top - typeDP(20)
        headerCard_v.requestLayout()
        stickyScroll(0)


        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
            { permissions ->
                // Handle Permission granted/rejected
                var permissionGranted = true
                permissions.entries.forEach {
                    if (it.key in REQUIRED_PERMISSIONS && !it.value)
                        permissionGranted = false
                }
                if (!permissionGranted) {
                    Toast.makeText(context, "Permission request denied", Toast.LENGTH_SHORT).show()
                }
            }
            .launch(REQUIRED_PERMISSIONS)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.menu_main, menu)

            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    R.id.menu_settings -> {
                        // clearCompletedTasks()
                        true
                    }

                    else -> false
                }
            }
        })

        btnSettings_v.setOnClickListener{
            findNavController().navigate(R.id.action_mainMenu_to_wiFiScan)
        }

        return view
    }

    fun typeDP(d: Float) : Float {
        val r = resources
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, d, r.displayMetrics)
    }

    fun typeDP(d: Int) : Int {
        val r = resources
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, d.toFloat(), r.displayMetrics).toInt()
    }

    fun getScreenWidth(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = requireActivity().windowManager.currentWindowMetrics
            val bounds: Rect = windowMetrics.bounds
            val insets: Insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars()
            )
            if (requireActivity().resources.configuration.orientation
                == Configuration.ORIENTATION_LANDSCAPE
                && requireActivity().resources.configuration.smallestScreenWidthDp < 600
            ) { // landscape and phone
                val navigationBarSize: Int = insets.right + insets.left
                bounds.width() - navigationBarSize
            } else { // portrait or tablet
                bounds.width()
            }
        } else {
            val outMetrics = DisplayMetrics()
            requireActivity().windowManager.defaultDisplay.getMetrics(outMetrics)
            outMetrics.widthPixels
        }
    }

    fun getScreenHeight(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = requireActivity().windowManager.currentWindowMetrics
            val insets: Insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars()
            )
            val bounds: Rect = windowMetrics.bounds
            if (requireActivity().resources.configuration.orientation
                == Configuration.ORIENTATION_LANDSCAPE
                && requireActivity().resources.configuration.smallestScreenWidthDp < 600
            ) { // landscape and phone
                bounds.height()
            } else { // portrait or tablet
                val navigationBarSize: Int = insets.bottom + insets.top
                bounds.height() - navigationBarSize
            }
        } else {
            val outMetrics = DisplayMetrics()
            requireActivity().windowManager.defaultDisplay.getMetrics(outMetrics)
            outMetrics.heightPixels
        }
    }

    fun getScreenInsets(): Insets {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = requireActivity().windowManager.currentWindowMetrics
            windowMetrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars()
            )
        } else {
            Insets.NONE
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }


}