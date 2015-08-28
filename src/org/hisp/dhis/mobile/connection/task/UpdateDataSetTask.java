package org.hisp.dhis.mobile.connection.task;

/*
 * Copyright (c) 2004-2013, University of Oslo All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met: * Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer. * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or other materials provided with the
 * distribution. * Neither the name of the HISP project nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Vector;

import javax.microedition.rms.RecordStoreException;

import org.hisp.dhis.mobile.connection.ConnectionManager;
import org.hisp.dhis.mobile.log.LogMan;
import org.hisp.dhis.mobile.midlet.DHISMIDlet;
import org.hisp.dhis.mobile.model.DataSet;
import org.hisp.dhis.mobile.model.DataSetList;
import org.hisp.dhis.mobile.model.OrgUnit;
import org.hisp.dhis.mobile.recordstore.DataSetRecordStore;
import org.hisp.dhis.mobile.recordstore.SettingRecordStore;
import org.hisp.dhis.mobile.util.SerializationUtil;

public class UpdateDataSetTask
    extends AbstractTask
{
    private static final String CLASS_TAG = "UpdateDataSetTask";

    public void run()
    {
        DataInputStream inputStream = null;
        try
        {
            DataSetList dataSetList = new DataSetList();
            Vector currentDataSets = DataSetRecordStore.loadDataSets( ConnectionManager.getOrgUnit() );
            for ( int i = 0; i < currentDataSets.size(); i++ )
            {
                ((DataSet) currentDataSets.elementAt( i )).setSections( null );
            }
            dataSetList.setCurrentDataSets( currentDataSets );

            inputStream = this.getDecompressedStream( this.upload( SerializationUtil.serialize( dataSetList ) ) );

            if ( inputStream != null )
            {
                dataSetList = null;
                dataSetList = new DataSetList();
                dataSetList.deSerialize( inputStream );

                if ( dataSetList.getAddedDataSets() != null )
                {
                    DataSetRecordStore.saveDataSets( dataSetList.getAddedDataSets(), ConnectionManager.getOrgUnit() );
                }

                if ( dataSetList.getModifiedDataSets() != null )
                {
                    DataSetRecordStore.saveDataSets( dataSetList.getModifiedDataSets(), ConnectionManager.getOrgUnit() );
                }

                Vector deletedDataSetVector = dataSetList.getDeletedDataSets();

                if ( deletedDataSetVector != null )
                {
                    for ( int i = 0; i < deletedDataSetVector.size(); i++ )
                    {
                        DataSetRecordStore.deleteDataSet( ((DataSet) deletedDataSetVector.elementAt( i )) );
                    }
                }
                
                try
                {
                    SettingRecordStore settingRecordStore = new SettingRecordStore();
                    OrgUnit orgUnit = ConnectionManager.getOrgUnit();
                    ConnectionManager.init( ConnectionManager.getDhisMIDlet(),
                        orgUnit.getDownloadFacilityReportUrl(),
                        settingRecordStore.get( SettingRecordStore.USERNAME ),
                        settingRecordStore.get( SettingRecordStore.PASSWORD ), DHISMIDlet.DEFAULT_LOCALE, orgUnit );
                    ConnectionManager.downloadDataSetValues();
                }
                catch ( RecordStoreException e )
                {
                    e.printStackTrace();
                }
            }
        }
        catch ( RecordStoreException e )
        {
            e.printStackTrace();
            LogMan.log( "RMS," + CLASS_TAG, e );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            LogMan.log( "Network," + CLASS_TAG, e );
        }
        finally
        {
            try
            {
                if ( inputStream != null )
                    inputStream.close();
                System.gc();
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }
        }
    }

}
