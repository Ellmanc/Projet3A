package com.projet3a.smartspectro

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.choices.*

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class Choices : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.choices, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        buttonCalibrate.setOnClickListener {
            var i = Intent(this.context, WavelengthCalibrationActivity::class.java)
            startActivity(i)
        }
    }

    }