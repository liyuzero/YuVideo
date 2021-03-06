package com.yu.video.tool

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.transition.TransitionInflater
import android.util.DisplayMetrics
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.yu.bundles.monitorfragment.MAEActivityResultListener
import com.yu.bundles.monitorfragment.MAEMonitorFragment
import java.lang.reflect.InvocationTargetException

class HostActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val display = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(display)
        val displayMetrics = resources.displayMetrics
        displayMetrics.density = display.widthPixels / 360f
        displayMetrics.densityDpi = (160 * displayMetrics.density).toInt()

        setContentView(R.layout.common_activity_host)

        var bundle: Bundle? = intent.getBundleExtra("params")
        if (bundle == null) {
            val key = intent.getStringExtra("paramsKey")
            if (!TextUtils.isEmpty(key)) {
                bundle = mParamsKeyMap[key!!]
                mParamsKeyMap.remove(key)
            }
        }

        val fragmentName = intent.getStringExtra("fragment_class_name")

        try {
            val fragment =
                    Class.forName(fragmentName!!).getConstructor().newInstance() as Fragment
            if (bundle != null) {
                fragment.arguments = bundle
            }

            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(R.id.contentContainer, fragment)
            transaction.commitAllowingStateLoss()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
    }

    override fun onBackPressed() {
        if (mHostActivityListener != null && mHostActivityListener!!.onBackPressed()) {
            return
        }
        super.onBackPressed()
    }

    var mHostActivityListener: HostActivityListener? = null

    interface HostActivityListener {
        fun onBackPressed(): Boolean
    }

    fun startFragmentWithAnim(shareView: View, fragment: Fragment, bundle: Bundle?) {
        if(bundle != null) {
            fragment.arguments = bundle
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fragment.sharedElementEnterTransition =
                TransitionInflater.from(shareView.context).inflateTransition(android.R.transition.move)
        }
        supportFragmentManager
            .beginTransaction()
            .addSharedElement(shareView, ViewCompat.getTransitionName(shareView)!!)
            .replace(R.id.contentContainer, fragment)
            .addToBackStack(null)
            .commit();
    }
}

private val mParamsKeyMap = HashMap<String, Bundle?>()

fun startFragment(context: Context, fragmentClass: Class<out Fragment>) {
    startFragment(context, fragmentClass, null)
}

fun startFragment(context: Context, fragmentClass: Class<out Fragment>, bundle: Bundle?) {
    startFragment(context, fragmentClass, bundle, false)
}

fun startFragment(context: Context, fragmentClass: Class<out Fragment>, bundle: Bundle?, isBigData: Boolean) {
    val intent = Intent(context, HostActivity::class.java)
    if (bundle != null) {
        if (isBigData) {
            val paramsKey = "" + bundle.hashCode() + " = " + System.currentTimeMillis()
            mParamsKeyMap[paramsKey] = bundle
            intent.putExtra("paramsKey", paramsKey)
        } else {
            intent.putExtra("params", bundle)
        }
    }
    intent.putExtra("fragment_class_name", fragmentClass.name)
    context.startActivity(intent)
}

fun startFragmentForResult(
        context: Context,
        fragmentClass: Class<out Fragment>,
        bundle: Bundle?,
        requestCode: Int,
        listener: MAEActivityResultListener
) {
    startFragmentForResult(
        context,
        fragmentClass,
        bundle,
        requestCode,
        false,
        listener
    )
}

fun startFragmentForResult(
        context: Context,
        fragmentClass: Class<out Fragment>,
        bundle: Bundle?,
        requestCode: Int,
        isBigData: Boolean,
        listener: MAEActivityResultListener
) {
    val intent = Intent(context, HostActivity::class.java)
    if (bundle != null) {
        if (isBigData) {
            val paramsKey = "" + bundle.hashCode() + " = " + System.currentTimeMillis()
            mParamsKeyMap[paramsKey] = bundle
            intent.putExtra("paramsKey", paramsKey)
        } else {
            intent.putExtra("params", bundle)
        }
    }
    intent.putExtra("fragment_class_name", fragmentClass.name)
    context.startActivity(intent)
    MAEMonitorFragment.getInstance(context as FragmentActivity)
            .startActivityForResult(intent, requestCode, listener)
}