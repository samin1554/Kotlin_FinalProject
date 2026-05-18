package com.example.student_pomodoro

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.View
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

class SlideInItemAnimator : DefaultItemAnimator() {

    init {
        addDuration = 250
        removeDuration = 200
        moveDuration = 250
        changeDuration = 200
    }

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        holder.itemView.alpha = 0f
        holder.itemView.translationX = holder.itemView.width * 0.3f
        dispatchAddStarting(holder)
        val animator = ObjectAnimator.ofPropertyValuesHolder(
            holder.itemView,
            android.animation.PropertyValuesHolder.ofFloat(View.ALPHA, 1f),
            android.animation.PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f)
        )
        animator.duration = addDuration
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                dispatchAddFinished(holder)
            }
            override fun onAnimationStart(animation: Animator) {
                dispatchAddStarting(holder)
            }
        })
        animator.start()
        return true
    }

    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
        dispatchRemoveStarting(holder)
        val animator = ObjectAnimator.ofPropertyValuesHolder(
            holder.itemView,
            android.animation.PropertyValuesHolder.ofFloat(View.ALPHA, 0f),
            android.animation.PropertyValuesHolder.ofFloat(View.TRANSLATION_X, -holder.itemView.width * 0.3f)
        )
        animator.duration = removeDuration
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                holder.itemView.translationX = 0f
                dispatchRemoveFinished(holder)
            }
            override fun onAnimationStart(animation: Animator) {
                dispatchRemoveStarting(holder)
            }
        })
        animator.start()
        return true
    }
}
