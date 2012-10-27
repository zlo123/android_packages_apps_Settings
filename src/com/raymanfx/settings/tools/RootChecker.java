/*
 * Copyright (C) 2012 RaymanFX (raymanfx@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.raymanfx.settings.tools;

import java.io.DataOutputStream;

import android.util.Log;

public class RootChecker {
	public static boolean runRootCommand(String command) {
		Process process = null;
		DataOutputStream os = null;
		try {
		process = Runtime.getRuntime().exec("su");
		os = new DataOutputStream(process.getOutputStream());
		os.writeBytes(command+"\n");
		os.writeBytes("exit\n");
		os.flush();
		process.waitFor();
		} catch (Exception e) {
		Log.d("com.raymanfx.settings.extras", "An error occured, please read the log file: "+e.getMessage());
		return false;
		}
		finally {
		try {
		if (os != null) os.close();
		process.destroy();
		} catch (Exception e) {}
		}
		return true;
		}

		public static boolean isRoot(){
		return runRootCommand("echo 'Success!'");
		}
		}
