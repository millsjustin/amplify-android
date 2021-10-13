/**
 * Copyright 2015-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 * <p>
 * http://aws.amazon.com/apache2.0
 * <p>
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.storage.s3.transfer

import android.os.Parcel
import android.os.Parcelable

/**
 * Configuration information used by the TransferHelper
 */
data class TransferOptions(var transferThreadPoolSize: Int = DEFAULT_THREAD_POOL_SIZE,
                           var connectionType: TransferNetworkConnectionType = DEFAULT_TRANSFER_CONNECTION_TYPE) : Parcelable {
    constructor(source: Parcel) : this(
        source.readInt()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeInt(transferThreadPoolSize)
    }

    companion object {
        val DEFAULT_THREAD_POOL_SIZE = 2 * (Runtime.getRuntime().availableProcessors() + 1)
        val DEFAULT_TRANSFER_CONNECTION_TYPE = TransferNetworkConnectionType.ANY

        @JvmField
        val CREATOR: Parcelable.Creator<TransferOptions> =
            object : Parcelable.Creator<TransferOptions> {
                override fun createFromParcel(source: Parcel): TransferOptions =
                    TransferOptions(source)

                override fun newArray(size: Int): Array<TransferOptions?> =
                    arrayOfNulls(size)
            }
    }
}
