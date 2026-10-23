package com.momentum.app
import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
object SuggestionImages {
 private const val MAX_ENCODED=700000
 fun valid(value:String):Boolean {
  if(value.isEmpty())return true
  if(value.length>MAX_ENCODED)return false
  return runCatching {val bytes=Base64.decode(value,Base64.NO_WRAP);val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds);bounds.outWidth in 1..1280 && bounds.outHeight in 1..1280}.getOrDefault(false)
 }
 fun bitmap(value:String):Bitmap?=runCatching{val bytes=Base64.decode(value,Base64.NO_WRAP);BitmapFactory.decodeByteArray(bytes,0,bytes.size)}.getOrNull()
 fun import(context:Context,uri:Uri):String {
  val bytes=context.contentResolver.openInputStream(uri)!!.use{input->
   val output=ByteArrayOutputStream();val buffer=ByteArray(8192)
   while(true){val n=input.read(buffer);if(n<0)break;require(output.size()+n<=10000000);output.write(buffer,0,n)};output.toByteArray()
  }
  val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
  require(bounds.outWidth>0 && bounds.outHeight>0)
  var sample=1;while(maxOf(bounds.outWidth,bounds.outHeight)/sample>2560)sample*=2
  val original=BitmapFactory.decodeByteArray(bytes,0,bytes.size,BitmapFactory.Options().apply{inSampleSize=sample})?:error("Unsupported image")
  val ratio=minOf(1f,1280f/maxOf(original.width,original.height))
  val scaled=Bitmap.createScaledBitmap(original,(original.width*ratio).toInt().coerceAtLeast(1),(original.height*ratio).toInt().coerceAtLeast(1),true)
  return try {val output=ByteArrayOutputStream();scaled.compress(Bitmap.CompressFormat.JPEG,75,output);val encoded=Base64.encodeToString(output.toByteArray(),Base64.NO_WRAP);require(valid(encoded));encoded}finally{if(scaled!==original)scaled.recycle();original.recycle()}
 }
}
fun githubDraftUrl(idea:DevelopmentIdea):android.net.Uri=android.net.Uri.parse("https://github.com/Uriel1010/momentum-android/issues/new").buildUpon()
 .appendQueryParameter("title",idea.title)
 .appendQueryParameter("body","Category: ${idea.category}\nPriority: ${idea.priority}\n\n${idea.notes.take(5000)}\n\nCreated from Momentum. Screenshots must be attached manually after review.").build()
