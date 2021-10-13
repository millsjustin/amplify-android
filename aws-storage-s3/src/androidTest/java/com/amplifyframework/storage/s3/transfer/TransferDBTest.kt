/**
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import junit.framework.TestCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.*

open class TransferDBTest: TestCase() {

    private lateinit var transferDB: TransferDB
    private lateinit var transferDatabaseHelper: TransferDatabaseHelper
    private lateinit var tempFile: File
    private val bucketName = "bucket_name"
    private val fileKey = "file_key"

    @Before
    override fun setUp() {
        super.setUp()
        val context = ApplicationProvider.getApplicationContext<Context>()
        transferDB = TransferDB(context)
        transferDatabaseHelper = TransferDatabaseHelper(context)
        tempFile = File.createTempFile("tempFile", ".txt")
    }

    @After
    override fun tearDown() {
        super.tearDown()
        transferDB.closeDB()
        tempFile.delete()
    }

    @Test
    fun testInsertSingleTransferRecord() {
        val uri = transferDB.insertSingleTransferRecord(
            TransferType.UPLOAD,
            bucketName,
            fileKey,
            tempFile,
            null,
            null
        )

        getInsertedRecord(uri)?.run{
            assertEquals(TransferType.UPLOAD, this.type)
            assertEquals(tempFile, File(this.file!!))
            assertEquals(fileKey, this.key)
            assertEquals(bucketName, this.bucketName)
        } ?: fail("InsertedRecord is null")


    }

    @Test
    fun testMultiPartUploadRecord() {
        val uploadID = UUID.randomUUID().toString()
        val uri = transferDB.insertMultipartUploadRecord(
            bucketName,
            fileKey,
            tempFile,
            1L,
            1,
            uploadID,
            1L,
            1,
            null
        )

        getInsertedRecord(uri)?.run {
            assertEquals(TransferType.UPLOAD, this.type)
            assertEquals(tempFile, File(this.file!!))
            assertEquals(fileKey, this.key)
            assertEquals(bucketName, this.bucketName)
            assertEquals(uploadID, this.multipartId)
        } ?: fail("InsertedRecord is null")
    }


    private fun getInsertedRecord(uri: Uri): TransferRecord? {
        val queryResult = transferDB.queryTransferById(uri.lastPathSegment?.toInt() ?: 0)
        var resultRecord: TransferRecord? = null
        queryResult?.let{
            while (it.moveToNext()) {
                val id: Int = it.getInt(it.getColumnIndexOrThrow(TransferTable.COLUMN_ID))
                resultRecord = TransferRecord.updateFromDB(it)
            }
        }
        return resultRecord
    }


}