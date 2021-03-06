package com.projet3a.smartspectro

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.wavelengthcalibrationchoices.*

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class WavelengthCalibrationChoices : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.wavelengthcalibrationchoices, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var value: String
        buttonSemiAuto.setOnClickListener {
            val i = Intent(this.context, WavelengthCalibrationActivity::class.java)
            value = buttonSemiAuto.text as String
            AppParameters.getInstance().button = value
            startActivity(i)
        }
        buttonAutomatique.setOnClickListener {
            val i = Intent(this.context, WavelengthCalibrationActivity::class.java)
            value = buttonAutomatique.text as String
            AppParameters.getInstance().button = value
            startActivity(i)
        }
        buttonManuel.setOnClickListener {
            val i = Intent(this.context, WavelengthCalibrationActivity::class.java)
            value = buttonManuel.text as String
            AppParameters.getInstance().button = value
            startActivity(i)
        }
    }

    }