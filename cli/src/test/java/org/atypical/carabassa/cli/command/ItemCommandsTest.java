package org.atypical.carabassa.cli.command;

import org.atypical.carabassa.cli.service.DatasetApiService;
import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItemCommandsTest {

    private final DatasetApiService api = mock(DatasetApiService.class);

    private int run(Object command, String... args) {
        ReflectionTestUtils.setField(command, "datasetApiService", api);
        return new CommandLine(command).execute(args);
    }

    private List<ItemRepresentation> items(int n) {
        List<ItemRepresentation> list = new ArrayList<>();
        for (long i = 1; i <= n; i++) {
            ItemRepresentation item = mock(ItemRepresentation.class);
            when(item.getId()).thenReturn(i);
            list.add(item);
        }
        return list;
    }

    @Test
    void deleteWithoutSearchNorAllIsRejected() throws Exception {
        assertEquals(ExitCode.USAGE, run(new DeleteItemCommand(), "-d", "ds", "-f"));
        verify(api, never()).deleteItem(anyLong(), anyLong());
        verify(api, never()).findItems(anyLong());
    }

    @Test
    void deleteWithBlankSearchAndNoAllIsRejected() throws Exception {
        assertEquals(ExitCode.USAGE, run(new DeleteItemCommand(), "-d", "ds", "-s", "", "-f"));
        assertEquals(ExitCode.USAGE, run(new DeleteItemCommand(), "-d", "ds", "-s", " ", "-f"));
        verify(api, never()).deleteItem(anyLong(), anyLong());
        verify(api, never()).findItems(anyLong());
        verify(api, never()).findItems(anyLong(), anyString());
    }

    @Test
    void deleteAllWithForceDeletesEveryItemWithoutPrompt() throws Exception {
        when(api.findByName("ds")).thenReturn(1L);
        List<ItemRepresentation> list = items(250);
        when(api.findItems(1L)).thenReturn(list);
        java.io.InputStream in = System.in;
        System.setIn(new ByteArrayInputStream(new byte[0]));
        try {
            assertEquals(ExitCode.OK, run(new DeleteItemCommand(), "-d", "ds", "--all", "-f"));
        } finally {
            System.setIn(in);
        }
        verify(api, org.mockito.Mockito.times(250)).deleteItem(anyLong(), anyLong());
    }

    @Test
    void deleteAllWithoutForceAndDeclinedConfirmationDeletesNothing() throws Exception {
        when(api.findByName("ds")).thenReturn(1L);
        List<ItemRepresentation> list = items(3);
        when(api.findItems(1L)).thenReturn(list);
        java.io.InputStream in = System.in;
        System.setIn(new ByteArrayInputStream("n\n".getBytes()));
        try {
            assertEquals(ExitCode.OK, run(new DeleteItemCommand(), "-d", "ds", "--all"));
        } finally {
            System.setIn(in);
        }
        verify(api, never()).deleteItem(anyLong(), anyLong());
    }

    @Test
    void deleteWithSearchUsesTheSearch() throws Exception {
        when(api.findByName("ds")).thenReturn(1L);
        List<ItemRepresentation> list = items(2);
        when(api.findItems(1L, "a=b")).thenReturn(list);
        assertEquals(ExitCode.OK, run(new DeleteItemCommand(), "-d", "ds", "-s", "a=b", "-f"));
        verify(api, org.mockito.Mockito.times(2)).deleteItem(anyLong(), anyLong());
        verify(api, never()).findItems(1L);
    }

    @Test
    void reindexWithoutSearchReindexesAllItems() throws Exception {
        when(api.findByName("ds")).thenReturn(1L);
        List<ItemRepresentation> list = items(150);
        when(api.findItems(1L)).thenReturn(list);
        assertEquals(ExitCode.OK, run(new ReindexItemsCommand(), "-d", "ds"));
        verify(api, org.mockito.Mockito.times(150)).reindex(anyLong(), anyLong());
    }
}
