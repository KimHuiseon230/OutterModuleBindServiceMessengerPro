package com.example.outtermodulebindservicemessengerpro

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.outtermodulebindservicemessengerpro.databinding.ActivityMainBinding
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    // 공통적인 선언
    lateinit var receiveMessenger: Messenger
    lateinit var sendMessenger: Messenger
    lateinit var messengerConnection: ServiceConnection

    //코루틴을 실행할 객체를 생성
    var processCoroutineJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        /* 1. receiveMessenger 생성한다. (이미 정의해놓은 객체를 전달 =HandlerReplayMsg)
         * 10번이 돌아오면 프로그래스 바를 진행
         * 메세지 20번을 보냄
         * 서비스를 취소하고 코루틴을 종료한다.
     receiveMessenger = Messenger(HandlerReplayMsg())*/

        //2. 서비스 커넥션을 생성한다.
        messengerConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                sendMessenger = Messenger(service)
                //10 번 명령을 전달 및 receiveMessenger 메세지를 전달
                val message = Message()
                // 내가 받을 메신저 를 담아서 보낸다. (미리 설계를 잘해야하는 부분)
                message.replyTo = receiveMessenger
                message.what = 10
                sendMessenger.send(message)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Toast.makeText(applicationContext, "서비스 종료", Toast.LENGTH_SHORT).show()
            }
        }

        //3. play 버튼을 누르면 bindService 서비스를 시작한다.
        binding.messengerPlay.setOnClickListener {
            val intent = Intent("ACTION_SERVICE_MESSENGER")
            intent.setPackage("com.example.mp3servicemessengerpro")
            bindService(intent, messengerConnection, Context.BIND_AUTO_CREATE)
        }
        //4. 정지버튼을 누르면 20 서비스에 전송해서 노래를 정지하고 bindservice를 종료하고 , 코루틴을 종료ㅎ
        binding.messengerStop.setOnClickListener {
            val message = Message()
            message.what = 20
            sendMessenger.send(message)
            unbindService(messengerConnection)
            //코루틴이 돌고 있다면
            processCoroutineJob?.cancel()
            //0으로 초기화
            binding.messengerProgress.progress = 0

        }

    }

    //  서비스에서 전송해올 메세지를 받기위한 핸들러
    inner class HandlerReplayMsg : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                10 -> {
                    // 만약 10이 오면 프로그래스 바를 코루틴 으로 처리 및 실행한다.
                    val bundle = msg.obj as Bundle // 다운 캐스팅
                    // 해당되는 객체가 있다면 조건을 건다.
                    val duration = bundle.getInt("duration")
                    if (duration > 10) {
                        // 프로그래스 바를 화면에서 만들면 최대치를 50이라 하면 거기까지 그려준다.
                        binding.messengerProgress.max = duration
                        val backgroundScope = CoroutineScope(Dispatchers.Default + Job())
                        processCoroutineJob = backgroundScope.launch {
                            // 스레드
                            while (binding.messengerProgress.progress < binding.messengerProgress.max) {
                                delay(1000)// 1초 지연. 증가하는 것을 보여주고, 50초가 걸리는 걸 보여줘야함
                                // 당연하게도 ANR이 발생 -> 그래서 코루틴으로 처리
                                binding.messengerProgress.incrementProgressBy(1000) //1초만 큼만 증가 시킨다.
                            } //end of while
                            //0으로 만들어서 초기화 시킨다...
                            binding.messengerProgress.progress = 0

                            // 종료 메세지를 서비스에 전달해야한다.
                            val message = Message()
                            message.what = 20
                            sendMessenger.send(message)

                            //서비스를 끊어버림
                            unbindService(messengerConnection) // onDistory
                            processCoroutineJob?.cancel()
                        }// end of backgroundScope
                    }// end of 10
                }
            }// end of when
        }// end of handleMessage
    }// end of HandlerReplayMsg

    //실행을 눌렀을때 작업 필요 ->
}

//                            unbindService(메신저 커낵션)
